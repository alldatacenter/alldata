-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_upd_by_id
END
IF (OBJECT_ID('x_user_FK_cred_store_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_cred_store_id
END
IF (OBJECT_ID('x_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_added_by_id
END
IF (OBJECT_ID('x_trx_log_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_trx_log] DROP CONSTRAINT x_trx_log_FK_upd_by_id
END
IF (OBJECT_ID('x_trx_log_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_trx_log] DROP CONSTRAINT x_trx_log_FK_added_by_id
END
IF (OBJECT_ID('x_resource_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_upd_by_id
END
IF (OBJECT_ID('x_resource_FK_parent_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_parent_id
END
IF (OBJECT_ID('x_resource_FK_asset_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_asset_id
END
IF (OBJECT_ID('x_resource_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_added_by_id
END
IF (OBJECT_ID('x_portal_user_role_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_user_id
END
IF (OBJECT_ID('x_portal_user_role_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_upd_by_id
END
IF (OBJECT_ID('x_portal_user_role_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_added_by_id
END
IF (OBJECT_ID('x_portal_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT x_portal_user_FK_upd_by_id
END
IF (OBJECT_ID('x_portal_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT x_portal_user_FK_added_by_id
END
IF (OBJECT_ID('x_policy_export_audit_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_export_audit] DROP CONSTRAINT x_policy_export_audit_FK_upd_by_id
END
IF (OBJECT_ID('x_policy_export_audit_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_export_audit] DROP CONSTRAINT x_policy_export_audit_FK_added_by_id
END
IF (OBJECT_ID('x_perm_map_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_user_id
END
IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_upd_by_id
END
IF (OBJECT_ID('x_perm_map_FK_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_res_id
END
IF (OBJECT_ID('x_perm_map_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_group_id
END
IF (OBJECT_ID('x_perm_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_added_by_id
END
IF (OBJECT_ID('x_group_users_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_user_id
END
IF (OBJECT_ID('x_group_users_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_upd_by_id
END
IF (OBJECT_ID('x_group_users_FK_p_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_p_group_id
END
IF (OBJECT_ID('x_group_users_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_added_by_id
END
IF (OBJECT_ID('x_group_groups_FK_p_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_p_group_id
END
IF (OBJECT_ID('x_group_groups_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_group_id
END
IF (OBJECT_ID('x_group_groups_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_added_by_id
END
IF (OBJECT_ID('x_group_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_upd_by_id
END
IF (OBJECT_ID('x_group_FK_cred_store_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_cred_store_id
END
IF (OBJECT_ID('x_group_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_added_by_id
END
IF (OBJECT_ID('x_db_base_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_db_base] DROP CONSTRAINT x_db_base_FK_upd_by_id
END
IF (OBJECT_ID('x_db_base_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_db_base] DROP CONSTRAINT x_db_base_FK_added_by_id
END
IF (OBJECT_ID('x_cred_store_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_cred_store] DROP CONSTRAINT x_cred_store_FK_upd_by_id
END
IF (OBJECT_ID('x_cred_store_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_cred_store] DROP CONSTRAINT x_cred_store_FK_added_by_id
END
IF (OBJECT_ID('x_auth_sess_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_user_id
END
IF (OBJECT_ID('x_auth_sess_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_upd_by_id
END
IF (OBJECT_ID('x_auth_sess_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_added_by_id
END
IF (OBJECT_ID('x_audit_map_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_user_id
END
IF (OBJECT_ID('x_audit_map_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_upd_by_id
END
IF (OBJECT_ID('x_audit_map_FK_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_res_id
END
IF (OBJECT_ID('x_audit_map_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_group_id
END
IF (OBJECT_ID('x_audit_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_added_by_id
END
IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_asset_FK_upd_by_id] DROP CONSTRAINT x_asset_FK_upd_by_id
END
IF (OBJECT_ID('x_asset_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_asset] DROP CONSTRAINT x_asset_FK_added_by_id
END
IF (OBJECT_ID('x_service_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_def] DROP CONSTRAINT x_service_def_FK_added_by_id
END
IF (OBJECT_ID('x_service_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_def] DROP CONSTRAINT x_service_def_FK_upd_by_id
END
IF (OBJECT_ID('x_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_added_by_id
END
IF (OBJECT_ID('x_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_upd_by_id
END
IF (OBJECT_ID('x_service_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_type
END
IF (OBJECT_ID('x_policy_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_added_by_id
END
IF (OBJECT_ID('x_policy_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_upd_by_id
END
IF (OBJECT_ID('x_policy_FK_service') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_service
END
IF (OBJECT_ID('x_service_config_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_config_def] DROP CONSTRAINT x_service_config_def_FK_defid
END
IF (OBJECT_ID('x_resource_def_FK_parent') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource_def] DROP CONSTRAINT x_resource_def_FK_parent
END
IF (OBJECT_ID('x_resource_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource_def] DROP CONSTRAINT x_resource_def_FK_defid
END
IF (OBJECT_ID('x_access_type_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_access_type_def] DROP CONSTRAINT x_access_type_def_FK_defid
END
IF (OBJECT_ID('x_atd_grants_FK_atdid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_access_type_def_grants] DROP CONSTRAINT x_atd_grants_FK_atdid
END
IF (OBJECT_ID('x_policy_condition_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_condition_def] DROP CONSTRAINT x_policy_condition_def_FK_defid
END
IF (OBJECT_ID('x_enum_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_enum_def] DROP CONSTRAINT x_enum_def_FK_defid
END
IF (OBJECT_ID('x_policy_ref_resource_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_resource_FK_resource_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_FK_resource_def_id
END
IF (OBJECT_ID('x_policy_ref_resource_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_resource] DROP CONSTRAINT x_policy_ref_resource_UK
END
IF (OBJECT_ID('x_policy_ref_resource') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_resource]
END
IF (OBJECT_ID('x_policy_ref_access_type_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_access_type_FK_access_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_FK_access_def_id
END
IF (OBJECT_ID('x_policy_ref_access_type_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_access_type] DROP CONSTRAINT x_policy_ref_access_type_UK
END
IF (OBJECT_ID('x_policy_ref_access_type') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_access_type]
END
IF (OBJECT_ID('x_policy_ref_condition_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_condition_FK_condition_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_FK_condition_def_id
END
IF (OBJECT_ID('x_policy_ref_condition_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_condition] DROP CONSTRAINT x_policy_ref_condition_UK
END
IF (OBJECT_ID('x_policy_ref_condition') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_condition]
END
IF (OBJECT_ID('x_policy_ref_datamask_type_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_datamask_type_FK_datamask_def_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_FK_datamask_def_id
END
IF (OBJECT_ID('x_policy_ref_datamask_type_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_datamask_type] DROP CONSTRAINT x_policy_ref_datamask_type_UK
END
IF (OBJECT_ID('x_policy_ref_datamask_type') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_datamask_type]
END
IF (OBJECT_ID('x_policy_ref_user_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_user_FK_user_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_FK_user_id
END
IF (OBJECT_ID('x_policy_ref_user_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_user] DROP CONSTRAINT x_policy_ref_user_UK
END
IF (OBJECT_ID('x_policy_ref_user') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_user]
END
IF (OBJECT_ID('x_policy_ref_group_FK_policy_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_FK_policy_id
END
IF (OBJECT_ID('x_policy_ref_group_FK_group_id') IS NOT NULL)
BEGIN
  ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_FK_group_id
END
IF (OBJECT_ID('x_policy_ref_group_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_ref_group] DROP CONSTRAINT x_policy_ref_group_UK
END
IF (OBJECT_ID('x_policy_ref_group') IS NOT NULL)
BEGIN
  DROP TABLE [dbo].[x_policy_ref_group]
END
IF (OBJECT_ID('x_enum_element_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_enum_element_def] DROP CONSTRAINT x_enum_element_def_FK_defid
END
IF (OBJECT_ID('x_service_config_map_FK_') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_config_map] DROP CONSTRAINT x_service_config_map_FK_
END
IF (OBJECT_ID('x_policy_resource_FK_policy_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource] DROP CONSTRAINT x_policy_resource_FK_policy_id
END
IF (OBJECT_ID('x_policy_resource_FK_res_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource] DROP CONSTRAINT x_policy_resource_FK_res_def_id
END
IF (OBJECT_ID('x_policy_resource_map_FK_resource_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource_map] DROP CONSTRAINT x_policy_resource_map_FK_resource_id
END
IF (OBJECT_ID('x_policy_item_FK_policy_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item] DROP CONSTRAINT x_policy_item_FK_policy_id
END
IF (OBJECT_ID('x_policy_item_access_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_access] DROP CONSTRAINT x_policy_item_access_FK_pi_id
END
IF (OBJECT_ID('x_policy_item_access_FK_atd_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_access] DROP CONSTRAINT x_policy_item_access_FK_atd_id
END
IF (OBJECT_ID('x_policy_item_condition_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_condition] DROP CONSTRAINT x_policy_item_condition_FK_pi_id
END
IF (OBJECT_ID('x_policy_item_condition_FK_pcd_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_condition] DROP CONSTRAINT x_policy_item_condition_FK_pcd_id
END
IF (OBJECT_ID('x_policy_item_user_perm_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_user_perm] DROP CONSTRAINT x_policy_item_user_perm_FK_pi_id
END
IF (OBJECT_ID('x_policy_item_user_perm_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_user_perm] DROP CONSTRAINT x_policy_item_user_perm_FK_user_id
END
IF (OBJECT_ID('x_policy_item_group_perm_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_group_perm] DROP CONSTRAINT x_policy_item_group_perm_FK_pi_id
END
IF (OBJECT_ID('x_policy_item_group_perm_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_group_perm] DROP CONSTRAINT x_policy_item_group_perm_FK_group_id
END
IF (OBJECT_ID('x_tag_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_def] DROP CONSTRAINT x_tag_def_FK_added_by_id
END
IF (OBJECT_ID('x_tag_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_def] DROP CONSTRAINT x_tag_def_FK_upd_by_id
END
IF (OBJECT_ID('x_tag_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_added_by_id
END
IF (OBJECT_ID('x_tag_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_upd_by_id
END
IF (OBJECT_ID('x_tag_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_type
END
IF (OBJECT_ID('x_service_res_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_service_id
END
IF (OBJECT_ID('x_service_res_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_added_by_id
END
IF (OBJECT_ID('x_service_res_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_upd_by_id
END
IF (OBJECT_ID('x_tag_res_map_FK_tag_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_tag_id
END
IF (OBJECT_ID('x_tag_attr_FK_tag_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_res_id
END
IF (OBJECT_ID('x_tag_res_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_added_by_id
END
IF (OBJECT_ID('x_tag_res_map_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_upd_by_id
END
IF (OBJECT_ID('x_datamask_type_def_FK_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_def_id
END
IF (OBJECT_ID('x_datamask_type_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_added_by_id
END
IF (OBJECT_ID('x_datamask_type_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_datamask_type_def] DROP CONSTRAINT x_datamask_type_def_FK_upd_by_id
END
IF (OBJECT_ID('x_policy_item_datamask_FK_policy_item_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_policy_item_id
END
IF (OBJECT_ID('x_policy_item_datamask_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_type
END
IF (OBJECT_ID('x_policy_item_datamask_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_added_by_id
END
IF (OBJECT_ID('x_policy_item_datamask_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_datamask] DROP CONSTRAINT x_policy_item_datamask_FK_upd_by_id
END
IF (OBJECT_ID('x_policy_item_rowfilter_FK_policy_item_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_policy_item_id
END
IF (OBJECT_ID('x_policy_item_rowfilter_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_added_by_id
END
IF (OBJECT_ID('x_policy_item_rowfilter_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_rowfilter] DROP CONSTRAINT x_policy_item_rowfilter_FK_upd_by_id
END
IF (OBJECT_ID('x_service_version_info_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_version_info] DROP CONSTRAINT x_service_version_info_service_id
END
IF (OBJECT_ID('x_group_UK_group_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_UK_group_name
END
IF (OBJECT_ID('x_plugin_info_UK') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_plugin_info] DROP CONSTRAINT x_plugin_info_UK
END
IF (OBJECT_ID('x_policy$x_policy_UK_name_service_zone') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy$x_policy_UK_name_service_zone
END
IF (OBJECT_ID('x_sz_ref_admin_group_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_added_by_id
END
IF (OBJECT_ID('x_sz_ref_group_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_upd_by_id
END
IF (OBJECT_ID('x_sz_ref_group_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_zone_id
END
IF (OBJECT_ID('x_sz_ref_group_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_group_id
END
IF (OBJECT_ID('x_sz_ref_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_added_by_id
END
IF (OBJECT_ID('x_sz_ref_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_upd_by_id
END
IF (OBJECT_ID('x_sz_ref_user_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_zone_id
END
IF (OBJECT_ID('x_sz_ref_user_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_user_id
END
IF (OBJECT_ID('x_sz_ref_user_FK_user_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_user_name
END
IF (OBJECT_ID('x_sz_ref_resource_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_added_by_id
END
IF (OBJECT_ID('x_sz_ref_resource_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_upd_by_id
END
IF (OBJECT_ID('x_sz_ref_resource_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_zone_id
END
IF (OBJECT_ID('x_sz_ref_resource_FK_resource_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_resource_def_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_added_by_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_upd_by_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_zone_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_service_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_service_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_service_name
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_added_by_id
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_upd_by_id
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_zone_id
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_tag_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_tag_service_id
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_tag_service_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_tag_service_name
END
IF (OBJECT_ID('x_ranger_global_state_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_ranger_global_state] DROP CONSTRAINT x_ranger_global_state_FK_upd_by_id
END
IF (OBJECT_ID('x_ranger_global_state_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_ranger_global_state] DROP CONSTRAINT x_ranger_global_state_FK_added_by_id
END
IF (OBJECT_ID('x_security_zone_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone] DROP CONSTRAINT x_security_zone_FK_added_by_id
END
IF (OBJECT_ID('x_security_zone_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone] DROP CONSTRAINT x_security_zone_FK_upd_by_id
END
IF (OBJECT_ID('vx_trx_log') IS NOT NULL)
BEGIN
    DROP VIEW [dbo].[vx_trx_log]
END
IF (OBJECT_ID('x_rms_mapping_provider') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_mapping_provider]
END
IF (OBJECT_ID('x_rms_resource_mapping') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_resource_mapping]
END
IF (OBJECT_ID('x_rms_notification') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_notification]
END
IF (OBJECT_ID('x_rms_service_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_rms_service_resource]
END
IF (OBJECT_ID('x_tag_change_log') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_change_log]
END
IF (OBJECT_ID('x_role_ref_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_role]
END

IF (OBJECT_ID('x_policy_ref_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_role]
END

IF (OBJECT_ID('x_role_ref_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_group]
END

IF (OBJECT_ID('x_role_ref_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role_ref_user]
END

IF (OBJECT_ID('x_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_role]
END
IF (OBJECT_ID('x_policy_change_log') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_change_log]
END
IF (OBJECT_ID('x_security_zone_ref_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_resource]
END
IF (OBJECT_ID('x_policy_ref_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_group]
END
IF (OBJECT_ID('x_policy_ref_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_user]
END
IF (OBJECT_ID('x_policy_ref_datamask_type') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_datamask_type]
END
IF (OBJECT_ID('x_policy_ref_condition') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_condition]
END
IF (OBJECT_ID('x_policy_ref_access_type') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_access_type]
END
IF (OBJECT_ID('x_policy_ref_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_ref_resource]
END
IF (OBJECT_ID('x_ugsync_audit_info') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_ugsync_audit_info]
END
IF (OBJECT_ID('x_policy_label_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_label_map]
END
IF (OBJECT_ID('x_policy_label') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_label]
END
IF (OBJECT_ID('x_plugin_info') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_plugin_info]
END
IF (OBJECT_ID('x_service_version_info') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_version_info]
END
IF (OBJECT_ID('x_policy_item_rowfilter') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_rowfilter]
END
IF (OBJECT_ID('x_policy_item_datamask') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_datamask]
END
IF (OBJECT_ID('x_datamask_type_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_datamask_type_def]
END
IF (OBJECT_ID('x_tag_resource_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_resource_map]
END
IF (OBJECT_ID('x_service_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_resource]
END
IF (OBJECT_ID('x_tag') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag]
END
IF (OBJECT_ID('x_tag_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_def]
END
IF (OBJECT_ID('x_group_module_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group_module_perm]
END
IF (OBJECT_ID('x_user_module_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_user_module_perm]
END
IF (OBJECT_ID('x_modules_master') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_modules_master]
END
IF (OBJECT_ID('x_data_hist') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_data_hist]
END
IF (OBJECT_ID('x_policy_item_group_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_group_perm]
END
IF (OBJECT_ID('x_policy_item_user_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_user_perm]
END
IF (OBJECT_ID('x_policy_item_condition') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_condition]
END
IF (OBJECT_ID('x_policy_item_access') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_access]
END
IF (OBJECT_ID('x_policy_item') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item]
END
IF (OBJECT_ID('x_policy_resource_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_resource_map]
END
IF (OBJECT_ID('x_policy_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_resource]
END
IF (OBJECT_ID('x_service_config_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_config_map]
END
IF (OBJECT_ID('x_enum_element_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_enum_element_def]
END
IF (OBJECT_ID('x_enum_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_enum_def]
END
IF (OBJECT_ID('x_context_enricher_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_context_enricher_def]
END
IF (OBJECT_ID('x_policy_condition_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_condition_def]
END
IF (OBJECT_ID('x_access_type_def_grants') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_access_type_def_grants]
END
IF (OBJECT_ID('x_access_type_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_access_type_def]
END
IF (OBJECT_ID('x_resource_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_resource_def]
END
IF (OBJECT_ID('x_service_config_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_config_def]
END
IF (OBJECT_ID('x_policy') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy]
END
IF (OBJECT_ID('x_security_zone_ref_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_group]
END
IF (OBJECT_ID('x_security_zone_ref_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_user]
END
IF (OBJECT_ID('x_security_zone_ref_tag_srvc') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_tag_srvc]
END
IF (OBJECT_ID('x_security_zone_ref_service') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_service]
END
IF (OBJECT_ID('x_ranger_global_state') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_ranger_global_state]
END
IF (OBJECT_ID('x_security_zone') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone]
END
IF (OBJECT_ID('x_service') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service]
END
IF (OBJECT_ID('x_service_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_def]
END
IF (OBJECT_ID('x_audit_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_audit_map]
END
IF (OBJECT_ID('x_perm_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_perm_map]
END
IF (OBJECT_ID('x_trx_log') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_trx_log]
END
IF (OBJECT_ID('x_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_resource]
END
IF (OBJECT_ID('x_policy_export_audit') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_export_audit]
END
IF (OBJECT_ID('x_group_users') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group_users]
END
IF (OBJECT_ID('x_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_user]
END
IF (OBJECT_ID('x_group_groups') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group_groups]
END
IF (OBJECT_ID('x_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group]
END
IF (OBJECT_ID('x_db_base') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_db_base]
END
IF (OBJECT_ID('x_cred_store') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_cred_store]
END
IF (OBJECT_ID('x_auth_sess') IS NOT NULL)
BEGIN
   DROP TABLE [dbo].[x_auth_sess]
END
IF (OBJECT_ID('x_asset') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_asset]
END
IF (OBJECT_ID('xa_access_audit') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[xa_access_audit]
END
IF (OBJECT_ID('x_portal_user_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_portal_user_role]
END
IF (OBJECT_ID('x_portal_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_portal_user]
END
IF (OBJECT_ID('x_db_version_h') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_db_version_h]
END


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_db_version_h](
        [id]	[bigint] identity(1,1) NOT NULL,
        [version]	[varchar](64) NOT NULL,
        [inst_at]	[datetime2] NOT NULL,
        [inst_by]	[varchar](256) NOT NULL,
        [updated_at]	[datetime2] NOT NULL,
        [updated_by]	[varchar](256) NOT NULL,
        [active]	[varchar](1) check(active IN ('Y', 'N')) DEFAULT 'Y',
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_portal_user](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [first_name] [nvarchar](256) DEFAULT NULL NULL,
        [last_name] [nvarchar](256) DEFAULT NULL NULL,
        [pub_scr_name] [varchar](2048) DEFAULT NULL NULL,
        [login_id] [nvarchar](767) DEFAULT NULL NULL,
        [password] [varchar](512) NOT NULL,
        [email] [varchar](512) DEFAULT NULL NULL,
        [status] [int] DEFAULT 0 NOT NULL,
        [user_src] [int] DEFAULT 0 NOT NULL,
        [notes] [varchar](4000) DEFAULT NULL NULL,
        [other_attributes] [varchar](4000) DEFAULT NULL NULL,
        [sync_source] [varchar](4000) DEFAULT NULL NULL,
        [old_passwords] [nvarchar](max) DEFAULT NULL,
        [password_updated_time] [datetime2] DEFAULT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_portal_user$x_portal_user_UK_login_id] UNIQUE NONCLUSTERED
(
        [login_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_portal_user_role](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [user_id] [bigint] NOT NULL,
        [user_role] [varchar](128) DEFAULT NULL NULL,
        [status] [int] DEFAULT 0  NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[xa_access_audit](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [audit_type] [int] DEFAULT 0 NOT NULL,
        [access_result] [int] DEFAULT 0 NULL,
        [access_type] [varchar](255) DEFAULT NULL NULL,
        [acl_enforcer] [varchar](255) DEFAULT NULL NULL,
        [agent_id] [varchar](255) DEFAULT NULL NULL,
        [client_ip] [varchar](255) DEFAULT NULL NULL,
        [client_type] [varchar](255) DEFAULT NULL NULL,
        [policy_id] [bigint] DEFAULT 0 NULL,
        [repo_name] [varchar](255) DEFAULT NULL NULL,
        [repo_type] [int] DEFAULT 0 NULL,
        [result_reason] [varchar](255) DEFAULT NULL NULL,
        [session_id] [varchar](255) DEFAULT NULL NULL,
        [event_time] [datetime2] DEFAULT NULL NULL,
        [request_user] [varchar](255) DEFAULT NULL NULL,
        [action] [varchar](2000) DEFAULT NULL NULL,
        [request_data] [varchar](4000) DEFAULT NULL NULL,
        [resource_path] [varchar](4000) DEFAULT NULL NULL,
        [resource_type] [varchar](255) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_asset](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [asset_name] [varchar](1024) NOT NULL,
        [descr] [varchar](4000) NOT NULL,
        [act_status] [int] DEFAULT 0 NOT NULL,
        [asset_type] [int] DEFAULT 0 NOT NULL,
        [config] [nvarchar](max) NULL,
        [sup_native] [tinyint] DEFAULT 0 NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_auth_sess](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [login_id] [varchar](767) NOT NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
        [ext_sess_id] [varchar](512) DEFAULT NULL NULL,
        [auth_time] [datetime2] NOT NULL,
        [auth_status] [int] DEFAULT 0 NOT NULL,
        [auth_type] [int] DEFAULT 0 NOT NULL,
        [auth_provider] [int] DEFAULT 0 NOT NULL,
        [device_type] [int] DEFAULT 0 NOT NULL,
        [req_ip] [varchar](48) NOT NULL,
        [req_ua] [varchar](1024) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_cred_store](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [store_name] [varchar](1024) NOT NULL,
        [descr] [varchar](4000) NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
CREATE TABLE [dbo].[x_db_base](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_group](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [group_name] [nvarchar](767) NOT NULL,
        [descr] [nvarchar](4000) NOT NULL,
        [status] [int] DEFAULT 0  NOT NULL,
        [group_type] [int] DEFAULT 0 NOT NULL,
        [cred_store_id] [bigint] DEFAULT NULL NULL,
        [group_src] [int] DEFAULT 0 NOT NULL,
        [is_visible] [int] DEFAULT 1 NOT NULL,
        [other_attributes] [varchar](4000) DEFAULT NULL NULL,
        [sync_source] [varchar](4000) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_group$x_group_UK_group_name] UNIQUE NONCLUSTERED
(
        [group_name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_group_groups](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [group_name] [nvarchar](1024) NOT NULL,
        [p_group_id] [bigint] DEFAULT NULL  NULL,
        [group_id] [bigint] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_user](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [user_name] [nvarchar](767) NOT NULL,
        [descr] [nvarchar](4000) NOT NULL,
        [status] [int] DEFAULT 0 NOT NULL,
        [cred_store_id] [bigint] DEFAULT NULL NULL,
        [is_visible] [int] DEFAULT 1 NOT NULL,
        [other_attributes] [varchar](4000) DEFAULT NULL NULL,
        [sync_source] [varchar](4000) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_user$x_user_UK_user_name] UNIQUE NONCLUSTERED
(
        [user_name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_group_users](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [group_name] [nvarchar](767) NOT NULL,
        [p_group_id] [bigint] DEFAULT NULL NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [[x_group_users$x_group_users_UK_uid_gname] UNIQUE NONCLUSTERED
(
        [user_id] ASC,
	[group_name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_export_audit](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [client_ip] [varchar](255) NOT NULL,
        [agent_id] [varchar](255) DEFAULT NULL NULL,
        [req_epoch] [bigint] NOT NULL,
        [last_updated] [datetime2] DEFAULT NULL NULL,
        [repository_name] [varchar](1024) DEFAULT NULL NULL,
        [exported_json] [nvarchar](max) DEFAULT NULL NULL,
        [http_ret_code] [int] DEFAULT 0 NOT NULL,
        [cluster_name] [varchar](255) DEFAULT NULL NULL,
        [zone_name] [varchar](255) DEFAULT NULL NULL,
        [policy_version] [bigint] DEFAULT NULL NULL
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_resource](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [res_name] [varchar](4000) DEFAULT NULL  NULL,
        [descr] [varchar](4000) DEFAULT NULL  NULL,
        [res_type] [int] DEFAULT 0 NOT NULL,
        [asset_id] [bigint] NOT NULL,
        [parent_id] [bigint]  DEFAULT NULL NULL,
        [parent_path] [varchar](4000) DEFAULT NULL  NULL,
        [is_encrypt] [int] DEFAULT 0 NOT NULL,
        [is_recursive] [int] DEFAULT 0 NOT NULL,
        [res_group] [varchar](1024)  DEFAULT NULL NULL,
        [res_dbs] [nvarchar](max) NULL,
        [res_tables] [nvarchar](max) NULL,
        [res_col_fams] [nvarchar](max) NULL,
        [res_cols] [nvarchar](max) NULL,
        [res_udfs] [nvarchar](max) NULL,
        [res_status] [int] DEFAULT 1 NOT NULL,
        [table_type] [int] DEFAULT 0 NOT NULL,
        [col_type] [int] DEFAULT 0 NOT NULL,
        [policy_name] [varchar](500) DEFAULT NULL  NULL,
        [res_topologies] [nvarchar](max) DEFAULT NULL  NULL,
        [res_services] [nvarchar](max)  DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_resource$x_resource_UK_policy_name] UNIQUE NONCLUSTERED
(
        [policy_name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_trx_log](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [class_type] [int] DEFAULT 0 NOT NULL,
        [object_id] [bigint] DEFAULT NULL NULL,
        [parent_object_id] [bigint] DEFAULT NULL NULL,
        [parent_object_class_type] [int] DEFAULT 0 NOT NULL,
        [parent_object_name] [varchar](1024)DEFAULT NULL  NULL,
        [object_name] [varchar](1024) DEFAULT NULL NULL,
        [attr_name] [varchar](255) DEFAULT NULL NULL,
        [prev_val] [nvarchar](max) DEFAULT NULL NULL,
        [new_val] [nvarchar](max)DEFAULT NULL  NULL,
        [trx_id] [varchar](1024)DEFAULT NULL  NULL,
        [action] [varchar](255) DEFAULT NULL NULL,
        [sess_id] [varchar](512) DEFAULT NULL NULL,
        [req_id] [varchar](30) DEFAULT NULL NULL,
        [sess_type] [varchar](30) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_perm_map](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [perm_group] [varchar](1024) DEFAULT NULL NULL,
        [res_id] [bigint] DEFAULT NULL NULL,
        [group_id] [bigint] DEFAULT NULL NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
        [perm_for] [int] DEFAULT 0 NOT NULL,
        [perm_type] [int] DEFAULT 0 NOT NULL,
        [is_recursive] [int] DEFAULT 0 NOT NULL,
        [is_wild_card] [tinyint] DEFAULT 1 NOT NULL,
        [grant_revoke] [tinyint] DEFAULT 1 NOT NULL,
        [ip_address] [nvarchar](max) DEFAULT NULL  NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_audit_map](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [res_id] [bigint] DEFAULT NULL NULL,
        [group_id] [bigint] DEFAULT NULL NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
        [audit_type] [int] DEFAULT 0 NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service_def](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [display_name] [varchar](1024) DEFAULT NULL NULL,
        [impl_class_name] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_description] [varchar](1024) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 1 NULL,
        [def_options] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [type] [bigint] DEFAULT NULL NULL,
        [name] [varchar](255) DEFAULT NULL NULL,
        [display_name] [varchar](255) DEFAULT NULL NULL,
        [policy_version] [bigint] DEFAULT NULL NULL,
        [policy_update_time] [datetime2] DEFAULT NULL NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 0 NOT NULL,
        [tag_service] [bigint] DEFAULT NULL NULL,
        [tag_version] [bigint] DEFAULT 0 NOT NULL,
        [tag_update_time] [datetime2] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_service$x_service_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_security_zone](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[version] [bigint] DEFAULT NULL NULL,
	[name] [varchar](255) NOT NULL,
	[jsonData] [nvarchar](max) DEFAULT NULL NULL,
	[description] [varchar](1024) DEFAULT NULL NULL,
	PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_security_zone$x_security_zone_UK_name] UNIQUE NONCLUSTERED
(
	[name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_ranger_global_state](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[version] [bigint] DEFAULT NULL NULL,
	[state_name] [varchar](255) NOT NULL,
	[app_data] [varchar](255) DEFAULT NULL NULL,
	PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_ranger_global_state$x_ranger_global_state_UK_name] UNIQUE NONCLUSTERED
(
	[state_name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [service] [bigint] NOT NULL,
        [name] [varchar](512) NOT NULL,
        [policy_type] [int] DEFAULT 0 NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [resource_signature] [varchar](128) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 0 NOT NULL,
        [is_audit_enabled] [tinyint] DEFAULT 0 NOT NULL,
        [policy_options] [varchar](4000) DEFAULT NULL NULL,
        [policy_priority] [int] DEFAULT 0 NOT NULL,
		[policy_text] [nvarchar](max) DEFAULT NULL NULL,
		[zone_id] [bigint] DEFAULT 1 NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_policy$x_policy_UK_name_service_zone] UNIQUE NONCLUSTERED
(
        [name] ASC, [service] ASC, [zone_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_policy$x_policy_UK_guid_service_zone] UNIQUE NONCLUSTERED
(
        [guid] ASC, [service] ASC, [zone_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_policy$x_policy_UK_service_signature] UNIQUE NONCLUSTERED
(
        [service] ASC, [resource_signature] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service_config_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [type] [varchar](1024) DEFAULT NULL NULL,
        [sub_type] [varchar](1024) DEFAULT NULL NULL,
        [is_mandatory] [tinyint] DEFAULT 0 NOT NULL,
        [default_value] [varchar](1024) DEFAULT NULL NULL,
        [validation_reg_ex] [varchar](1024) DEFAULT NULL NULL,
        [validation_message] [varchar](1024) DEFAULT NULL NULL,
        [ui_hint] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_validation_message] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_resource_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [type] [varchar](1024) DEFAULT NULL NULL,
        [res_level] [bigint] DEFAULT NULL NULL,
        [parent] [bigint] DEFAULT NULL NULL,
        [mandatory] [tinyint] DEFAULT 0 NOT NULL,
        [look_up_supported] [tinyint] DEFAULT 0 NOT NULL,
        [recursive_supported] [tinyint] DEFAULT 0 NOT NULL,
        [excludes_supported] [tinyint] DEFAULT 0 NOT NULL,
        [matcher] [varchar](1024) DEFAULT NULL NULL,
        [matcher_options] [varchar](1024) DEFAULT NULL NULL,
        [validation_reg_ex] [varchar](1024) DEFAULT NULL NULL,
        [validation_message] [varchar](1024) DEFAULT NULL NULL,
        [ui_hint] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_validation_message] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        [datamask_options] [varchar](1024) DEFAULT NULL NULL,
        [rowfilter_options] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_access_type_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0 NULL,
        [datamask_options] [varchar](1024) DEFAULT NULL NULL,
        [rowfilter_options] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_access_type_def_grants](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [atd_id] [bigint] NOT NULL,
        [implied_grant] [varchar](1024) DEFAULT NULL NULL,
    PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_condition_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [evaluator] [varchar](1024) DEFAULT NULL NULL,
        [evaluator_options] [varchar](1024) DEFAULT NULL NULL,
        [validation_reg_ex] [varchar](1024) DEFAULT NULL NULL,
        [validation_message] [varchar](1024) DEFAULT NULL NULL,
        [ui_hint] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_description] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_validation_message] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
CREATE TABLE [dbo].[x_context_enricher_def](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint]NOT NULL,
        [item_id] [bigint]NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [enricher] [varchar](1024) DEFAULT NULL NULL,
        [enricher_options] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_enum_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [default_index] [bigint] DEFAULT NULL NULL,
    PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_enum_element_def] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [enum_def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) DEFAULT NULL NULL,
        [label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
    PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service_config_map] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [service] [bigint] NOT NULL,
        [config_key] [varchar](1024) DEFAULT NULL NULL,
        [config_value] [varchar](4000) DEFAULT NULL NULL,
    PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_resource] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_id] [bigint] NOT NULL,
        [res_def_id] [bigint] NOT NULL,
        [is_excludes] [tinyint] DEFAULT 0 NOT NULL,
        [is_recursive] [tinyint] DEFAULT 0 NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_resource_map] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [resource_id] [bigint] NOT NULL,
        [value] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_item] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_id] [bigint] NOT NULL,
        [delegate_admin] [tinyint] DEFAULT 0 NOT NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        [item_type] [int] DEFAULT 0 NOT NULL,
        [is_enabled] [tinyint] DEFAULT 1 NOT NULL,
        [comments] [varchar](255) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_item_access] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [type] [bigint] NOT NULL,
        [is_allowed] [tinyint] DEFAULT 0 NOT NULL,
        [sort_order] [int] DEFAULT 0 NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_item_condition] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [type] [bigint] NOT NULL,
        [value] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_item_user_perm] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [user_id] [bigint]  DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_item_group_perm] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](1024) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [group_id] [bigint]  DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0  NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_data_hist] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [obj_guid] [varchar](1024) NOT NULL,
        [obj_class_type] [int] NOT NULL,
        [obj_id] [bigint] NOT NULL,
        [obj_name] [varchar](1024) NOT NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [action] [varchar](512) NOT NULL,
        [from_time] [datetime2] NOT NULL,
        [to_time] [datetime2] DEFAULT NULL NULL,
        [content] [nvarchar](max) NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_modules_master] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [module] [varchar](1024)NOT NULL,
        [url] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_user_module_perm] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
        [module_id] [bigint] DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [is_allowed] [int] DEFAULT 1 NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_group_module_perm] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [group_id] [bigint] DEFAULT NULL NULL,
        [module_id] [bigint] DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [is_allowed] [int] DEFAULT 1 NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_tag_def](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [name] [varchar](255) NOT NULL,
        [source] [varchar](128) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 0 NOT NULL,
        [tag_attrs_def_text] [nvarchar](max) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_tag_def$x_tag_def_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_tag_def$x_tag_def_UK_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_tag](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [type] [bigint] NOT NULL,
        [owned_by] [smallint] DEFAULT 0 NOT NULL,
        [policy_options] [varchar](4000) DEFAULT NULL NULL,
        [tag_attrs_text] [nvarchar](max) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_tag$x_tag_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service_resource](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [service_id] [bigint] NOT NULL,
        [resource_signature] [varchar](128) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 1 NOT NULL,
        [service_resource_elements_text] [nvarchar](max) DEFAULT NULL NULL,
        [tags_text] [nvarchar](max) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_service_resource$x_service_res_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_service_resource$x_service_res_IDX_svc_id_resource_signature] UNIQUE NONCLUSTERED
(
        [service_id] ASC, [resource_signature] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_tag_resource_map](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [tag_id] [bigint] NOT NULL,
        [res_id] [bigint] NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_tag_resource_map$x_tag_resource_map_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_datamask_type_def](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [def_id] [bigint] NOT NULL,
        [item_id] [bigint] NOT NULL,
        [name] [varchar](1024) NOT NULL,
        [label] [varchar](1024) NOT NULL,
        [description] [varchar](1024) DEFAULT NULL NULL,
        [transformer] [varchar](1024) DEFAULT NULL NULL,
        [datamask_options] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_label] [varchar](1024) DEFAULT NULL NULL,
        [rb_key_description] [varchar](1024) DEFAULT NULL NULL,
        [sort_order] [int] DEFAULT 0 NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
CREATE TABLE [dbo].[x_policy_item_datamask](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [type] [bigint] NOT NULL,
        [condition_expr] [varchar](1024) DEFAULT NULL NULL,
        [value_expr] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
CREATE TABLE [dbo].[x_policy_item_rowfilter](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_item_id] [bigint] NOT NULL,
        [filter_expr] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_service_version_info](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [service_id] [bigint] NOT NULL,
        [policy_version] [bigint] NOT NULL DEFAULT 0,
        [policy_update_time] [datetime2] DEFAULT NULL NULL,
        [tag_version] [bigint] NOT NULL DEFAULT 0,
        [tag_update_time] [datetime2] DEFAULT NULL NULL,
        [role_version] [bigint] NOT NULL DEFAULT 0,
        [role_update_time] [datetime2] DEFAULT NULL NULL,
        [version] [bigint] NOT NULL DEFAULT 1,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_plugin_info](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [service_name] [varchar](255) NOT NULL,
        [app_type] [varchar](128) NOT NULL,
        [host_name] [varchar](255) NOT NULL,
        [ip_address] [varchar](64) NOT NULL,
        [info] [varchar](1024) NOT NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_plugin_info$x_plugin_info_UK] UNIQUE NONCLUSTERED
(
        [service_name] ASC, [host_name] ASC, [app_type] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_label] (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [label_name] [varchar](512) DEFAULT NULL,
        PRIMARY KEY CLUSTERED
        (
        [id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
        CONSTRAINT [x_policy_label$x_policy_label_UK_label_name] UNIQUE NONCLUSTERED
        (
        [label_name] ASC
        )WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE dbo.x_policy_label_map (
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) DEFAULT NULL NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [policy_id] [bigint] DEFAULT NULL,
        [policy_label_id] [bigint] DEFAULT NULL,
        PRIMARY KEY CLUSTERED
        (
        [id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_ugsync_audit_info](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [event_time] [datetime2] DEFAULT NULL NULL,
        [user_name] [nvarchar](255) NOT  NULL,
        [sync_source] [varchar](128) NOT NULL,
        [no_of_new_users] [bigint] NOT NULL,
        [no_of_new_groups] [bigint] NOT NULL,
        [no_of_modified_users] [bigint] NOT NULL,
        [no_of_modified_groups] [bigint] NOT NULL,
        [sync_source_info] [nvarchar](max) NOT NULL,
        [session_id] [varchar](255) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_resource] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [resource_def_id] [bigint] NOT NULL,
  [resource_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
  CONSTRAINT [x_policy_ref_resource$x_policy_ref_resource_UK] UNIQUE NONCLUSTERED
  (
	[policy_id] ASC, [resource_def_id] ASC
  )WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_access_type] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [access_def_id] [bigint] NOT NULL,
  [access_type_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_access_type$x_policy_ref_access_type_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [access_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_condition] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [condition_def_id] [bigint] NOT NULL,
  [condition_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_condition$x_policy_ref_condition_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [condition_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_datamask_type] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [datamask_def_id] [bigint] NOT NULL,
  [datamask_type_name] [varchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_datamask_type$x_policy_ref_datamask_type_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [datamask_def_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_user] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [user_id] [bigint] NOT NULL,
  [user_name] [nvarchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_user$x_policy_ref_user_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [user_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_group] (
  [id] [bigint] IDENTITY (1, 1) NOT NULL,
  [guid] [varchar](1024) DEFAULT NULL NULL,
  [create_time] [datetime2] DEFAULT NULL NULL,
  [update_time] [datetime2] DEFAULT NULL NULL,
  [added_by_id] [bigint] DEFAULT NULL NULL,
  [upd_by_id] [bigint] DEFAULT NULL NULL,
  [policy_id] [bigint] NOT NULL,
  [group_id] [bigint] NOT NULL,
  [group_name] [nvarchar](4000) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_group$x_policy_ref_group_UK] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [group_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_security_zone_ref_service](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [zone_id] [bigint] DEFAULT NULL NULL,
        [service_id] [bigint] DEFAULT NULL NULL,
        [service_name] [varchar](255) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_security_zone_ref_service] WITH CHECK ADD CONSTRAINT [x_sz_ref_service_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_service] WITH CHECK ADD CONSTRAINT [x_sz_ref_service_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_service] WITH CHECK ADD CONSTRAINT [x_sz_ref_service_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_service] WITH CHECK ADD CONSTRAINT [x_sz_ref_service_FK_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_service] WITH CHECK ADD CONSTRAINT [x_sz_ref_service_FK_service_name] FOREIGN KEY([service_name]) REFERENCES [dbo].[x_service] ([name])
GO
CREATE TABLE [dbo].[x_security_zone_ref_tag_srvc](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [zone_id] [bigint] DEFAULT NULL NULL,
        [tag_srvc_id] [bigint] DEFAULT NULL NULL,
        [tag_srvc_name] [varchar](255) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_tag_srvc_id] FOREIGN KEY([tag_srvc_id]) REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_tag_srvc_name] FOREIGN KEY([tag_srvc_name]) REFERENCES [dbo].[x_service] ([name])
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_security_zone_ref_resource](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [zone_id] [bigint] DEFAULT NULL NULL,
        [resource_def_id] [bigint] DEFAULT NULL NULL,
        [resource_name] [varchar](255) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_security_zone_ref_resource] WITH CHECK ADD CONSTRAINT [x_sz_ref_resource_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_resource] WITH CHECK ADD CONSTRAINT [x_sz_ref_resource_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_resource] WITH CHECK ADD CONSTRAINT [x_sz_ref_resource_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_resource] WITH CHECK ADD CONSTRAINT [x_sz_ref_resource_FK_resource_def_id] FOREIGN KEY([resource_def_id]) REFERENCES [dbo].[x_resource_def] ([id])
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_security_zone_ref_user](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [zone_id] [bigint] DEFAULT NULL NULL,
        [user_id] [bigint] DEFAULT NULL NULL,
        [user_name] [nvarchar](767) DEFAULT NULL NULL,
        [user_type] [tinyint] DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_user_id] FOREIGN KEY([user_id]) REFERENCES [dbo].[x_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_user] WITH CHECK ADD CONSTRAINT [x_sz_ref_user_FK_user_name] FOREIGN KEY([user_name]) REFERENCES [dbo].[x_user] ([user_name])
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_security_zone_ref_group](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [zone_id] [bigint] DEFAULT NULL NULL,
        [group_id] [bigint] DEFAULT NULL NULL,
        [group_name] [nvarchar](767) DEFAULT NULL NULL,
        [group_type] [tinyint] DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_security_zone_ref_group] WITH CHECK ADD CONSTRAINT [x_sz_ref_group_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_group] WITH CHECK ADD CONSTRAINT [x_sz_ref_group_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_group] WITH CHECK ADD CONSTRAINT [x_sz_ref_group_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_group] WITH CHECK ADD CONSTRAINT [x_sz_ref_group_FK_group_id] FOREIGN KEY([group_id]) REFERENCES [dbo].[x_group] ([id])
GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_change_log](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [service_id] [bigint] NOT NULL,
        [change_type] [int] NOT NULL,
        [policy_version] [bigint] DEFAULT 0 NOT NULL,
        [service_type] [varchar](256) DEFAULT NULL NULL,
        [policy_type] [int] DEFAULT NULL NULL,
        [zone_name] [varchar](256) DEFAULT NULL NULL,
        [policy_id] [bigint]  DEFAULT NULL NULL,
        [policy_guid] [varchar](1024) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_change_log_IDX_service_id] ON [x_policy_change_log]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_change_log_IDX_policy_version] ON [x_policy_change_log]
(
   [policy_version] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]

GO
CREATE UNIQUE NONCLUSTERED INDEX [x_policy_change_log_uk_service_id_policy_version] ON [x_policy_change_log]
(
   [service_id] ASC,[policy_version] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]

GO
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role](
 [id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[version] [bigint] DEFAULT NULL NULL,
[name] [nvarchar](255) NOT NULL,
[description] [nvarchar](1024) DEFAULT NULL NULL,
[role_options] [varchar](4000) DEFAULT NULL NULL,
[role_text] [nvarchar](max) DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],

 CONSTRAINT [x_role$x_role_UK_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role] WITH CHECK ADD CONSTRAINT [x_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role] WITH CHECK ADD CONSTRAINT [x_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_user](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[user_id] [bigint] DEFAULT NULL NULL,
[user_name] [nvarchar](767) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_user] WITH CHECK ADD CONSTRAINT [x_role_ref_user_FK_user_id] FOREIGN KEY([user_id]) REFERENCES [dbo].[x_user] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_group](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[group_id] [bigint] DEFAULT NULL NULL,
[group_name] [nvarchar](767) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
 PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_group] WITH CHECK ADD CONSTRAINT [x_role_ref_group_FK_group_id] FOREIGN KEY([group_id]) REFERENCES [dbo].[x_group] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_policy_ref_role](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[policy_id] [bigint] NOT NULL,
[role_id] [bigint] NOT NULL,
[role_name] [nvarchar](255) DEFAULT NULL NULL,
 PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
    CONSTRAINT [x_policy_ref_role$x_policy_ref_role_UK_polId_roleId] UNIQUE NONCLUSTERED
(
        [policy_id] ASC, [role_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_policy_id] FOREIGN KEY([policy_id]) REFERENCES [dbo].[x_policy] ([id])
GO
ALTER TABLE [dbo].[x_policy_ref_role] WITH CHECK ADD CONSTRAINT [x_policy_ref_role_FK_role_id] FOREIGN KEY([role_id]) REFERENCES [dbo].[x_role] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
CREATE TABLE [dbo].[x_role_ref_role](
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[update_time] [datetime2] DEFAULT NULL NULL,
[added_by_id] [bigint] DEFAULT NULL NULL,
[upd_by_id] [bigint] DEFAULT NULL NULL,
[role_ref_id] [bigint] DEFAULT NULL NULL,
[role_id] [bigint] NOT NULL,
[role_name] [nvarchar](255) DEFAULT NULL NULL,
[priv_type] [int] DEFAULT NULL NULL,
  PRIMARY KEY CLUSTERED
  (
  [id] ASC
  ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_role_ref_role] WITH CHECK ADD CONSTRAINT [x_role_ref_role_FK_role_ref_id] FOREIGN KEY([role_ref_id]) REFERENCES [dbo].[x_role] ([id])
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON

CREATE TABLE [dbo].[x_tag_change_log] (
[id] [bigint] IDENTITY(1,1) NOT NULL,
[create_time] [datetime2] DEFAULT NULL NULL,
[service_id] [bigint] NOT NULL,
[change_type] [int] NOT NULL,
[service_tags_version] [bigint] DEFAULT 0 NOT NULL,
[service_resource_id] [bigint]  DEFAULT NULL NULL,
[tag_id] [bigint]  DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_change_log_IDX_service_id] ON [x_tag_change_log]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_change_log_IDX_tag_version] ON [x_tag_change_log]
(
   [service_tags_version] ASC
)

WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE UNIQUE NONCLUSTERED INDEX [x_tag_change_log_uk_service_id_service_tags_version] ON [x_tag_change_log]
(
   [service_id] ASC, [service_tags_version] ASC
)

WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON

ALTER TABLE [dbo].[x_asset]  WITH CHECK ADD  CONSTRAINT [x_asset_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_asset] CHECK CONSTRAINT [x_asset_FK_added_by_id]
ALTER TABLE [dbo].[x_asset]  WITH CHECK ADD  CONSTRAINT [x_asset_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_asset] CHECK CONSTRAINT [x_asset_FK_upd_by_id]
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_added_by_id]
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_group_id]
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_res_id] FOREIGN KEY([res_id])
REFERENCES [dbo].[x_resource] ([id])
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_res_id]
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_upd_by_id]
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_user_id]
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_added_by_id]
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_upd_by_id]
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_user_id]
ALTER TABLE [dbo].[x_cred_store]  WITH CHECK ADD  CONSTRAINT [x_cred_store_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_cred_store] CHECK CONSTRAINT [x_cred_store_FK_added_by_id]
ALTER TABLE [dbo].[x_cred_store]  WITH CHECK ADD  CONSTRAINT [x_cred_store_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_cred_store] CHECK CONSTRAINT [x_cred_store_FK_upd_by_id]
ALTER TABLE [dbo].[x_db_base]  WITH CHECK ADD  CONSTRAINT [x_db_base_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_db_base] CHECK CONSTRAINT [x_db_base_FK_added_by_id]
ALTER TABLE [dbo].[x_db_base]  WITH CHECK ADD  CONSTRAINT [x_db_base_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_db_base] CHECK CONSTRAINT [x_db_base_FK_upd_by_id]
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_added_by_id]
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_cred_store_id] FOREIGN KEY([cred_store_id])
REFERENCES [dbo].[x_cred_store] ([id])
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_cred_store_id]
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_upd_by_id]
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_added_by_id]
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_group_id]
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_p_group_id] FOREIGN KEY([p_group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_p_group_id]
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_added_by_id]
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_p_group_id] FOREIGN KEY([p_group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_p_group_id]
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_upd_by_id]
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_user_id]
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_added_by_id]
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_group_id]
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_res_id] FOREIGN KEY([res_id])
REFERENCES [dbo].[x_resource] ([id])
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_res_id]
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_upd_by_id]
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_user_id]
ALTER TABLE [dbo].[x_policy_export_audit]  WITH CHECK ADD  CONSTRAINT [x_policy_export_audit_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_export_audit] CHECK CONSTRAINT [x_policy_export_audit_FK_added_by_id]
ALTER TABLE [dbo].[x_policy_export_audit]  WITH CHECK ADD  CONSTRAINT [x_policy_export_audit_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_export_audit] CHECK CONSTRAINT [x_policy_export_audit_FK_upd_by_id]
ALTER TABLE [dbo].[x_portal_user]  WITH CHECK ADD  CONSTRAINT [x_portal_user_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_portal_user] CHECK CONSTRAINT [x_portal_user_FK_added_by_id]
ALTER TABLE [dbo].[x_portal_user]  WITH CHECK ADD  CONSTRAINT [x_portal_user_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_portal_user] CHECK CONSTRAINT [x_portal_user_FK_upd_by_id]
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_added_by_id]
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_upd_by_id]
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_user_id]
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_added_by_id]
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_asset_id] FOREIGN KEY([asset_id])
REFERENCES [dbo].[x_asset] ([id])
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_asset_id]
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_parent_id] FOREIGN KEY([parent_id])
REFERENCES [dbo].[x_resource] ([id])
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_parent_id]
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_upd_by_id]
ALTER TABLE [dbo].[x_trx_log]  WITH CHECK ADD  CONSTRAINT [x_trx_log_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_trx_log] CHECK CONSTRAINT [x_trx_log_FK_added_by_id]
ALTER TABLE [dbo].[x_trx_log]  WITH CHECK ADD  CONSTRAINT [x_trx_log_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_trx_log] CHECK CONSTRAINT [x_trx_log_FK_upd_by_id]
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_added_by_id]
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_cred_store_id] FOREIGN KEY([cred_store_id])
REFERENCES [dbo].[x_cred_store] ([id])
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_cred_store_id]
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_upd_by_id]
ALTER TABLE [dbo].[x_user_module_perm]  WITH CHECK ADD  CONSTRAINT [x_user_module_perm_FK_moduleid] FOREIGN KEY([module_id])
REFERENCES [dbo].[x_modules_master] ([id])
ALTER TABLE [dbo].[x_user_module_perm]  WITH CHECK ADD  CONSTRAINT [x_user_module_perm_FK_userid] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_group_module_perm]  WITH CHECK ADD  CONSTRAINT [x_grp_module_perm_FK_module_id] FOREIGN KEY([module_id])
REFERENCES [dbo].[x_modules_master] ([id])
ALTER TABLE [dbo].[x_group_module_perm]  WITH CHECK ADD  CONSTRAINT [x_grp_module_perm_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_service_def]  WITH CHECK ADD  CONSTRAINT [x_service_def_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_def] CHECK CONSTRAINT [x_service_def_FK_added_by_id]
ALTER TABLE [dbo].[x_service_def]  WITH CHECK ADD  CONSTRAINT [x_service_def_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_def] CHECK CONSTRAINT [x_service_def_FK_upd_by_id]
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_added_by_id]
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_upd_by_id]
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_type] FOREIGN KEY([type])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_type]
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_tag_service] FOREIGN KEY([tag_service])
REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_added_by_id]
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_upd_by_id]
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_service] FOREIGN KEY([service])
REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_service]
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_config_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_config_def_FK_defid]
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_conf_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_conf_def_FK_added_by]
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_conf_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_conf_def_FK_upd_by]
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_parent] FOREIGN KEY([parent])
REFERENCES [dbo].[x_resource_def] ([id])
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_parent]
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_defid]
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_added_by]
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_upd_by]
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_defid]
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_added_by]
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_upd_by]
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_atd_grants_FK_atdid] FOREIGN KEY([atd_id])
REFERENCES [dbo].[x_access_type_def] ([id])
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_atd_grants_FK_atdid]
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_grants_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_access_type_def_grants_FK_added_by]
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_grants_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_access_type_def_grants_FK_upd_by]
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_defid]
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_added_by]
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_upd_by]
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_defid]
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_added_by_id]
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_upd_by_id]
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_defid]
ALTER TABLE [dbo].[x_enum_element_def]  WITH CHECK ADD  CONSTRAINT [x_enum_element_def_FK_defid] FOREIGN KEY([enum_def_id])
REFERENCES [dbo].[x_enum_def] ([id])
ALTER TABLE [dbo].[x_enum_element_def] CHECK CONSTRAINT [x_enum_element_def_FK_defid]
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_added_by]
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_upd_by]
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_] FOREIGN KEY([service])
REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_]
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_added_by]
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_upd_by]
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_policy_id] FOREIGN KEY([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_policy_id]
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_res_def_id] FOREIGN KEY([res_def_id])
REFERENCES [dbo].[x_resource_def] ([id])
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_res_def_id]
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_added_by]
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_upd_by]
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_resource_id] FOREIGN KEY([resource_id])
REFERENCES [dbo].[x_policy_resource] ([id])
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_resource_id]
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_added_by]
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_upd_by]
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_policy_id] FOREIGN KEY([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_policy_id]
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_added_by]
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_upd_by]
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_pi_id]
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_atd_id] FOREIGN KEY([type])
REFERENCES [dbo].[x_access_type_def] ([id])
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_atd_id]
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_added_by]
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_upd_by]
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_pi_id]
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_pcd_id] FOREIGN KEY([type])
REFERENCES [dbo].[x_policy_condition_def] ([id])
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_pcd_id]
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_added_by]
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_upd_by]
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_pi_id]
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_user_id]
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_added_by]
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_upd_by]
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_pi_id]
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_group_id]
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_added_by]
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_upd_by]
ALTER TABLE [dbo].[x_tag_def] WITH CHECK ADD CONSTRAINT [x_tag_def_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_tag_def] WITH CHECK ADD CONSTRAINT [x_tag_def_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_type] FOREIGN KEY([type]) REFERENCES [dbo].[x_tag_def] ([id])
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_tag_id] FOREIGN KEY([tag_id]) REFERENCES [dbo].[x_tag] ([id])
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_res_id] FOREIGN KEY([res_id]) REFERENCES [dbo].[x_service_resource] ([id])
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_def_id] FOREIGN KEY([def_id]) REFERENCES [dbo].[x_service_def] ([id])
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_datamask_type_def] WITH CHECK ADD CONSTRAINT [x_datamask_type_def_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_policy_item_id] FOREIGN KEY([policy_item_id]) REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_type] FOREIGN KEY([type]) REFERENCES [dbo].[x_datamask_type_def] ([id])
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_datamask] WITH CHECK ADD CONSTRAINT [x_policy_item_datamask_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_policy_item_id] FOREIGN KEY([policy_item_id]) REFERENCES [dbo].[x_policy_item] ([id])
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_item_rowfilter] WITH CHECK ADD CONSTRAINT [x_policy_item_rowfilter_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_service_version_info] WITH CHECK ADD CONSTRAINT [x_service_version_info_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_policy_label] WITH CHECK ADD CONSTRAINT [x_policy_label_FK_added_by_id] FOREIGN KEY ([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_label] WITH CHECK ADD CONSTRAINT x_policy_label_FK_upd_by_id FOREIGN KEY ([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_added_by_id] FOREIGN KEY ([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_upd_by_id] FOREIGN KEY ([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_policy_id] FOREIGN KEY ([policy_id]) REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map_FK_policy_label_id] FOREIGN KEY ([policy_label_id]) REFERENCES [dbo].[x_policy_label] ([id])
ALTER TABLE [dbo].[x_policy_label_map] WITH CHECK ADD CONSTRAINT [x_policy_label_map$x_policy_label_map_pid_plid] UNIQUE (policy_id, policy_label_id)
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_res_def_id] FOREIGN KEY ([resource_def_id])
REFERENCES [dbo].[x_resource_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_res_def_id]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_resource] CHECK CONSTRAINT [x_policy_ref_resource_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_resource] WITH CHECK ADD CONSTRAINT [x_policy_ref_resource_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_access_def_id] FOREIGN KEY ([access_def_id])
REFERENCES [dbo].[x_access_type_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_access_def_id]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_access_type] CHECK CONSTRAINT [x_policy_ref_access_type_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_access_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_access_type_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_res_def_id] FOREIGN KEY ([condition_def_id])
REFERENCES [dbo].[x_policy_condition_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_res_def_id]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_condition] CHECK CONSTRAINT [x_policy_ref_condition_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_condition] WITH CHECK ADD CONSTRAINT [x_policy_ref_condition_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_res_def_id] FOREIGN KEY ([datamask_def_id])
REFERENCES [dbo].[x_datamask_type_def] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_res_def_id]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_datamask_type] CHECK CONSTRAINT [x_policy_ref_datamask_type_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_datamask_type] WITH CHECK ADD CONSTRAINT [x_policy_ref_datamask_type_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_policy_id]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_user_id] FOREIGN KEY ([user_id])
REFERENCES [dbo].[x_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_user_id]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_added_by]

ALTER TABLE [dbo].[x_policy_ref_user] WITH CHECK ADD CONSTRAINT [x_policy_ref_user_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_user] CHECK CONSTRAINT [x_policy_ref_user_FK_upd_by]

ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_policy_id] FOREIGN KEY ([policy_id])
REFERENCES [dbo].[x_policy] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_policy_id]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_group_id] FOREIGN KEY ([group_id])
REFERENCES [dbo].[x_group] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_group_id]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_added_by] FOREIGN KEY ([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy_ref_group] CHECK CONSTRAINT [x_policy_ref_group_FK_added_by]
ALTER TABLE [dbo].[x_policy_ref_group] WITH CHECK ADD CONSTRAINT [x_policy_ref_group_FK_upd_by] FOREIGN KEY ([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_security_zone] WITH CHECK ADD CONSTRAINT [x_security_zone_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_security_zone] WITH CHECK ADD CONSTRAINT [x_security_zone_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_ranger_global_state] WITH CHECK ADD CONSTRAINT [x_ranger_global_state_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_ranger_global_state] WITH CHECK ADD CONSTRAINT [x_ranger_global_state_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
ALTER TABLE [dbo].[x_policy] WITH CHECK ADD CONSTRAINT [x_policy_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id])
GO
CREATE NONCLUSTERED INDEX [x_asset_cr_time] ON [x_asset]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_asset_FK_added_by_id] ON [x_asset]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_asset_FK_upd_by_id] ON [x_asset]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_asset_up_time] ON [x_asset]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_cr_time] ON [x_audit_map]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_FK_added_by_id] ON [x_audit_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_FK_group_id] ON [x_audit_map]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]

CREATE NONCLUSTERED INDEX [x_audit_map_FK_res_id] ON [x_audit_map]
(
   [res_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_FK_upd_by_id] ON [x_audit_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_FK_user_id] ON [x_audit_map]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_audit_map_up_time] ON [x_audit_map]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_auth_sess_cr_time] ON [x_auth_sess]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_added_by_id] ON [x_auth_sess]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_upd_by_id] ON [x_auth_sess]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_user_id] ON [x_auth_sess]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_auth_sess_up_time] ON [x_auth_sess]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_cred_store_cr_time] ON [x_cred_store]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_cred_store_FK_added_by_id] ON [x_cred_store]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_cred_store_FK_upd_by_id] ON [x_cred_store]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_cred_store_up_time] ON [x_cred_store]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_db_base_cr_time] ON [x_db_base]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_db_base_FK_added_by_id] ON [x_db_base]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_db_base_FK_upd_by_id] ON [x_db_base]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_db_base_up_time] ON [x_db_base]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_cr_time] ON [x_group]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_FK_added_by_id] ON [x_group]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_FK_cred_store_id] ON [x_group]
(
   [cred_store_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_FK_upd_by_id] ON [x_group]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_cr_time] ON [x_group_groups]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_FK_added_by_id] ON [x_group_groups]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_FK_group_id] ON [x_group_groups]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_FK_p_group_id] ON [x_group_groups]
(
   [p_group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_FK_upd_by_id] ON [x_group_groups]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_groups_up_time] ON [x_group_groups]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_up_time] ON [x_group]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_cr_time] ON [x_group_users]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_FK_added_by_id] ON [x_group_users]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_FK_p_group_id] ON [x_group_users]
(
   [p_group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_FK_upd_by_id] ON [x_group_users]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_FK_user_id] ON [x_group_users]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_group_users_up_time] ON [x_group_users]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_cr_time] ON [x_perm_map]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_FK_added_by_id] ON [x_perm_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_FK_group_id] ON [x_perm_map]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_FK_res_id] ON [x_perm_map]
(
   [res_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_FK_upd_by_id] ON [x_perm_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_FK_user_id] ON [x_perm_map]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_perm_map_up_time] ON [x_perm_map]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_export_audit_cr_time] ON [x_policy_export_audit]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_export_audit_FK_added_by_id] ON [x_policy_export_audit]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_export_audit_FK_upd_by_id] ON [x_policy_export_audit]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_export_audit_up_time] ON [x_policy_export_audit]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_cr_time] ON [x_portal_user]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_email] ON [x_portal_user]
(
   [email] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_FK_added_by_id] ON [x_portal_user]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_FK_upd_by_id] ON [x_portal_user]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_name] ON [x_portal_user]
(
   [first_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_role_cr_time] ON [x_portal_user_role]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_added_by_id] ON [x_portal_user_role]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_upd_by_id] ON [x_portal_user_role]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_user_id] ON [x_portal_user_role]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_role_up_time] ON [x_portal_user_role]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_portal_user_up_time] ON [x_portal_user]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_cr_time] ON [x_resource]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_FK_added_by_id] ON [x_resource]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_FK_asset_id] ON [x_resource]
(
   [asset_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_FK_parent_id] ON [x_resource]
(
   [parent_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_FK_upd_by_id] ON [x_resource]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_up_time] ON [x_resource]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_trx_log_cr_time] ON [x_trx_log]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_trx_log_FK_added_by_id] ON [x_trx_log]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_trx_log_FK_upd_by_id] ON [x_trx_log]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_trx_log_up_time] ON [x_trx_log]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_user_cr_time] ON [x_user]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_user_FK_added_by_id] ON [x_user]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_user_FK_cred_store_id] ON [x_user]
(
   [cred_store_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]

CREATE NONCLUSTERED INDEX [x_user_FK_upd_by_id] ON [x_user]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_user_up_time] ON [x_user]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [xa_access_audit_cr_time] ON [xa_access_audit]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [xa_access_audit_event_time] ON [xa_access_audit]
(
   [event_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [xa_access_audit_added_by_id] ON [xa_access_audit]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [xa_access_audit_upd_by_id] ON [xa_access_audit]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [xa_access_audit_up_time] ON [xa_access_audit]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_def_added_by_id] ON [x_service_def]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_def_upd_by_id] ON [x_service_def]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_def_cr_time] ON [x_service_def]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_def_up_time] ON [x_service_def]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_added_by_id] ON [x_service]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_upd_by_id] ON [x_service]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_cr_time] ON [x_service]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_up_time] ON [x_service]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_type] ON [x_service]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_added_by_id] ON [x_policy]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_upd_by_id] ON [x_policy]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_cr_time] ON [x_policy]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_up_time] ON [x_policy]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_service] ON [x_policy]
(
   [service] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_resource_signature] ON [x_policy]
(
   [resource_signature] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_def_parent] ON [x_resource_def]
(
   [parent] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_usr_module_perm_idx_moduleid] ON [x_user_module_perm]
(
   [module_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_usr_module_perm_idx_userid] ON [x_user_module_perm]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_grp_module_perm_idx_groupid] ON [x_group_module_perm]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_grp_module_perm_idx_moduleid] ON [x_group_module_perm]
(
   [module_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_def_IDX_added_by_id] ON [x_tag_def]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_def_IDX_upd_by_id] ON [x_tag_def]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_IDX_type] ON [x_tag]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_IDX_added_by_id] ON [x_tag]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_IDX_upd_by_id] ON [x_tag]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_res_IDX_added_by_id] ON [x_service_resource]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_res_IDX_upd_by_id] ON [x_service_resource]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_tag_id] ON [x_tag_resource_map]
(
   [tag_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_res_id] ON [x_tag_resource_map]
(
   [res_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_added_by_id] ON [x_tag_resource_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_upd_by_id] ON [x_tag_resource_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_config_def_IDX_def_id] ON [x_service_config_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_resource_def_IDX_def_id] ON [x_resource_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_access_type_def_IDX_def_id] ON [x_access_type_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_access_type_def_IDX_grants_atd_id] ON [x_access_type_def_grants]
(
   [atd_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_context_enricher_def_IDX_def_id] ON [x_context_enricher_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_enum_def_IDX_def_id] ON [x_enum_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_enum_element_def_IDX_enum_def_id] ON [x_enum_element_def]
(
   [enum_def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_config_map_IDX_service] ON [x_service_config_map]
(
   [service] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_resource_IDX_policy_id] ON [x_policy_resource]
(
   [policy_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_resource_IDX_res_def_id] ON [x_policy_resource]
(
   [res_def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_resource_map_IDX_resource_id] ON [x_policy_resource_map]
(
   [resource_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_IDX_policy_id] ON [x_policy_item]
(
   [policy_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_access_IDX_policy_item_id] ON [x_policy_item_access]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_access_IDX_type] ON [x_policy_item_access]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_condition_IDX_policy_item_id] ON [x_policy_item_condition]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_condition_IDX_type] ON [x_policy_item_condition]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_user_perm_IDX_policy_item_id] ON [x_policy_item_user_perm]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_user_perm_IDX_user_id] ON [x_policy_item_user_perm]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_group_perm_IDX_policy_item_id] ON [x_policy_item_group_perm]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_group_perm_IDX_group_id] ON [x_policy_item_group_perm]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_resource_IDX_service_id] ON [x_service_resource]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_datamask_type_def_IDX_def_id] ON [x_datamask_type_def]
(
   [def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_datamask_IDX_policy_item_id] ON [x_policy_item_datamask]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_item_rowfilter_IDX_policy_item_id] ON [x_policy_item_rowfilter]
(
   [policy_item_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_service_version_info_IDX_service_id] ON [x_service_version_info]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_plugin_info_IDX_service_name] ON [x_plugin_info]
(
   [service_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_plugin_info_IDX_host_name] ON [x_plugin_info]
(
   [host_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_policy_label_IDX_label_id] ON [dbo].[x_policy_label]
(
[id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_name ON [dbo].[x_policy_label]
(
[label_name] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX x_policy_label_IDX_label_map_id ON [dbo].[x_policy_label_map]
(
[id] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_ugsync_audit_info_etime] ON [x_ugsync_audit_info]
(
   [event_time] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_ugsync_audit_info_sync_src] ON [x_ugsync_audit_info]
(
   [sync_source] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_ugsync_audit_info_uname] ON [x_ugsync_audit_info]
(
   [user_name] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_data_hist_idx_objid_objclstype] ON [x_data_hist]
(
   [obj_id] ASC,[obj_class_type] ASC
)WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF EXISTS (SELECT *
           FROM   sys.objects
           WHERE  object_id = OBJECT_ID(N'dbo.getModulesIdByName')
                  AND type IN ( N'FN', N'IF', N'TF', N'FS', N'FT' ))
  DROP FUNCTION dbo.getModulesIdByName
  PRINT 'Dropped function dbo.getModulesIdByName'

GO
PRINT 'Creating function dbo.getModulesIdByName'
GO
CREATE FUNCTION dbo.getModulesIdByName
(

        @inputValue varchar(200)
)
RETURNS int
AS
BEGIN
        Declare @myid int;

        Select @myid = id from x_modules_master where module = @inputValue;

        return @myid;

END
GO

PRINT 'Created function dbo.getModulesIdByName successfully'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
IF EXISTS (SELECT *
           FROM   sys.objects
           WHERE  object_id = OBJECT_ID(N'dbo.getXportalUIdByLoginId')
                  AND type IN ( N'FN', N'IF', N'TF', N'FS', N'FT' ))
  DROP FUNCTION dbo.getXportalUIdByLoginId
  PRINT 'Dropped function dbo.getXportalUIdByLoginId'

GO
PRINT 'Creating function dbo.getXportalUIdByLoginId'
GO
CREATE FUNCTION dbo.getXportalUIdByLoginId
(

        @inputValue varchar(200)
)
RETURNS int
AS
BEGIN
        Declare @myid int;

        Select @myid = id from x_portal_user where x_portal_user.login_id = @inputValue;

        return @myid;

END
GO

PRINT 'Created function dbo.getXportalUIdByLoginId successfully'
GO

IF (OBJECT_ID('x_rms_service_res_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_service_resource] DROP CONSTRAINT x_rms_service_res_FK_service_id
END
GO
IF (OBJECT_ID('x_rms_notification_FK_hl_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_notification] DROP CONSTRAINT x_rms_notification_FK_hl_service_id
END
GO
IF (OBJECT_ID('x_rms_notification_FK_ll_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_notification] DROP CONSTRAINT x_rms_notification_FK_ll_service_id
END
GO
IF (OBJECT_ID('x_rms_res_map_FK_hl_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_resource_mapping] DROP CONSTRAINT x_rms_res_map_FK_hl_res_id
END
GO
IF (OBJECT_ID('x_rms_res_map_FK_ll_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_rms_resource_mapping] DROP CONSTRAINT x_rms_res_map_FK_ll_res_id
END
GO


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_rms_service_resource](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [guid] [varchar](64) NOT NULL,
        [create_time] [datetime2] DEFAULT NULL NULL,
        [update_time] [datetime2] DEFAULT NULL NULL,
        [added_by_id] [bigint] DEFAULT NULL NULL,
        [upd_by_id] [bigint] DEFAULT NULL NULL,
        [version] [bigint] DEFAULT NULL NULL,
        [service_id] [bigint] NOT NULL,
        [resource_signature] [varchar](128) DEFAULT NULL NULL,
        [is_enabled] [tinyint] DEFAULT 1 NOT NULL,
        [service_resource_elements_text] [nvarchar](max) DEFAULT NULL NULL,
        PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_rms_service_resource$x_service_res_UK_guid] UNIQUE NONCLUSTERED
(
        [guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_service_resource] WITH CHECK ADD CONSTRAINT [x_rms_service_res_FK_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_service_resource_IDX_service_id] ON [x_rms_service_resource]
(
   [service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_rms_service_resource_IDX_resource_signature] ON [x_rms_service_resource]
(
   [resource_signature] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO


SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_rms_notification](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [hms_name] [VARCHAR](128) NULL DEFAULT NULL,
        [notification_id] [bigint] NULL DEFAULT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [change_type] [VARCHAR](64) NULL DEFAULT  NULL,
        [hl_resource_id] [bigint] NULL DEFAULT NULL,
        [hl_service_id] [bigint] NULL DEFAULT NULL,
        [ll_resource_id] [bigint] NULL DEFAULT NULL,
        [ll_service_id] [bigint] NULL DEFAULT NULL,

PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_notification]  WITH CHECK ADD CONSTRAINT [x_rms_notification_FK_hl_service_id] FOREIGN KEY([hl_service_id])
REFERENCES [dbo].[x_service] ([id])
ALTER TABLE [dbo].[x_rms_notification]  WITH CHECK ADD CONSTRAINT [x_rms_notification_FK_ll_service_id] FOREIGN KEY([ll_service_id])
REFERENCES [dbo].[x_service] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_notification_id] ON [x_rms_notification]
(
   [notification_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_hms_name_notification_id] ON [x_rms_notification]
(
   [hms_name] ASC,[notification_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_hl_service_id] ON [x_rms_notification]
(
   [hl_service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
CREATE NONCLUSTERED INDEX [x_rms_notification_IDX_ll_service_id] ON [x_rms_notification]
(
   [ll_service_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[x_rms_resource_mapping](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [hl_resource_id] [bigint] NOT NULL,
        [ll_resource_id] [bigint] NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_rms_res_map_UK_hl_res_id_ll_res_id] UNIQUE NONCLUSTERED
(
        [hl_resource_id] ASC, [ll_resource_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_hl_res_id] FOREIGN KEY([hl_resource_id])
REFERENCES [dbo].[x_rms_service_resource] ([id])
ALTER TABLE [dbo].[x_rms_resource_mapping]  WITH CHECK ADD CONSTRAINT [x_rms_res_map_FK_ll_res_id] FOREIGN KEY([ll_resource_id])
REFERENCES [dbo].[x_rms_service_resource] ([id])
GO
CREATE NONCLUSTERED INDEX [x_rms_resource_mapping_IDX_hl_resource_id] ON [x_rms_resource_mapping]
(
   [hl_resource_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_rms_resource_mapping_IDX_ll_resource_id] ON [x_rms_resource_mapping]
(
   [ll_resource_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[x_rms_mapping_provider](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [change_timestamp] [datetime2] DEFAULT NULL NULL,
        [name] [VARCHAR](128) NOT NULL,
        [last_known_version] [bigint] NOT NULL,
PRIMARY KEY CLUSTERED
(
        [id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_rms_mapping_provider_UK_name] UNIQUE NONCLUSTERED
(
        [name] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1);
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),'ROLE_SYS_ADMIN',1);
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'admin',0,'Administrator');
insert into x_group (ADDED_BY_ID,CREATE_TIME,DESCR,GROUP_TYPE,GROUP_NAME,STATUS,UPDATE_TIME,UPD_BY_ID) values (dbo.getXportalUIdByLoginId('admin'),CURRENT_TIMESTAMP,'public group',0,'public',0,CURRENT_TIMESTAMP,1);
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Resource Based Policies','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Users/Groups','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Reports','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Audit','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Key Manager','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Tag Based Policies','');
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Security Zone','');
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1);
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('rangerusersync'),'ROLE_SYS_ADMIN',1);
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangerusersync',0,'rangerusersync');
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1);
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('keyadmin'),'ROLE_KEY_ADMIN',1);
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'keyadmin',0,'keyadmin');
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1);
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('rangertagsync'),'ROLE_SYS_ADMIN',1);
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangertagsync',0,'rangertagsync');
INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, dbo.getXportalUIdByLoginId('admin'), dbo.getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('CORE_DB_SCHEMA',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('016',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('018',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('019',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('020',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('021',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('022',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('023',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('024',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('025',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('026',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('027',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('028',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('029',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('030',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('031',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('032',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('033',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('034',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('035',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('036',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('037',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('038',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('039',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('040',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('041',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('042',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('043',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('044',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('045',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('046',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('047',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('048',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('049',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('050',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('051',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('052',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('053',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('054',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('055',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('056',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('057',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('058',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('059',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('DB_PATCHES',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Key Manager'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Reports'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Resource Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Tag Based Policies'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Users/Groups'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('keyadmin'),dbo.getModulesIdByName('Audit'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);

INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerRole','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerUserStore','{"Version":"1"}');
INSERT INTO x_ranger_global_state (create_time,update_time,added_by_id,upd_by_id,version,state_name,app_data) VALUES (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1,'RangerSecurityZone','{"Version":"1"}');

INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10001',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10002',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10003',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10004',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10005',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10006',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10007',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10008',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10009',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10010',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10011',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10012',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10013',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10014',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10015',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10016',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10017',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10019',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10020',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10025',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10026',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10027',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10028',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10030',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10033',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10034',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10035',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10036',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10037',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10038',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10040',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10041',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10043',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10044',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10045',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10046',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10047',CURRENT_TIMESTAMP,'Ranger 2.2.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10049',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10050',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10052',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10053',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10054',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10055',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('J10056',CURRENT_TIMESTAMP,'Ranger 3.0.0',CURRENT_TIMESTAMP,'localhost','Y');
INSERT INTO x_db_version_h (version,inst_at,inst_by,updated_at,updated_by,active) VALUES ('JAVA_PATCHES',CURRENT_TIMESTAMP,'Ranger 1.0.0',CURRENT_TIMESTAMP,'localhost','Y');
GO
CREATE VIEW [dbo].[vx_trx_log] AS
select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log
where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id)
GO
