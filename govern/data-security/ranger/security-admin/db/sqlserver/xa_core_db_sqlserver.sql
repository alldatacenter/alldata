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

GO
IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_user_FK_cred_store_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_cred_store_id
END
GO
IF (OBJECT_ID('x_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_user] DROP CONSTRAINT x_user_FK_added_by_id
END
GO
IF (OBJECT_ID('x_trx_log_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_trx_log] DROP CONSTRAINT x_trx_log_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_trx_log_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_trx_log] DROP CONSTRAINT x_trx_log_FK_added_by_id
END
GO
IF (OBJECT_ID('x_resource_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_resource_FK_parent_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_parent_id
END
GO
IF (OBJECT_ID('x_resource_FK_asset_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_asset_id
END
GO
IF (OBJECT_ID('x_resource_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource] DROP CONSTRAINT x_resource_FK_added_by_id
END
GO
IF (OBJECT_ID('x_portal_user_role_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_user_id
END
GO
IF (OBJECT_ID('x_portal_user_role_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_portal_user_role_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user_role] DROP CONSTRAINT x_portal_user_role_FK_added_by_id
END
GO
IF (OBJECT_ID('x_portal_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT x_portal_user_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_portal_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_portal_user] DROP CONSTRAINT x_portal_user_FK_added_by_id
END
GO
IF (OBJECT_ID('x_policy_export_audit_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_export_audit] DROP CONSTRAINT x_policy_export_audit_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_policy_export_audit_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_export_audit] DROP CONSTRAINT x_policy_export_audit_FK_added_by_id
END
GO
IF (OBJECT_ID('x_perm_map_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_user_id
END
GO
IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_perm_map_FK_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_res_id
END
GO
IF (OBJECT_ID('x_perm_map_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_group_id
END
GO
IF (OBJECT_ID('x_perm_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_perm_map] DROP CONSTRAINT x_perm_map_FK_added_by_id
END
GO
IF (OBJECT_ID('x_group_users_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_user_id
END
GO
IF (OBJECT_ID('x_group_users_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_group_users_FK_p_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_p_group_id
END
GO
IF (OBJECT_ID('x_group_users_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_users] DROP CONSTRAINT x_group_users_FK_added_by_id
END
GO
IF (OBJECT_ID('x_group_groups_FK_p_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_p_group_id
END
GO
IF (OBJECT_ID('x_group_groups_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_group_id
END
GO
IF (OBJECT_ID('x_group_groups_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group_groups] DROP CONSTRAINT x_group_groups_FK_added_by_id
END
GO
IF (OBJECT_ID('x_group_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_group_FK_cred_store_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_cred_store_id
END
GO
IF (OBJECT_ID('x_group_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_group] DROP CONSTRAINT x_group_FK_added_by_id
END
GO
IF (OBJECT_ID('x_db_base_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_db_base] DROP CONSTRAINT x_db_base_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_db_base_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_db_base] DROP CONSTRAINT x_db_base_FK_added_by_id
END
GO
IF (OBJECT_ID('x_cred_store_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_cred_store] DROP CONSTRAINT x_cred_store_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_cred_store_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_cred_store] DROP CONSTRAINT x_cred_store_FK_added_by_id
END
GO
IF (OBJECT_ID('x_auth_sess_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_user_id
END
GO
IF (OBJECT_ID('x_auth_sess_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_auth_sess_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_auth_sess] DROP CONSTRAINT x_auth_sess_FK_added_by_id
END
GO
IF (OBJECT_ID('x_audit_map_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_user_id
END
GO
IF (OBJECT_ID('x_audit_map_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_audit_map_FK_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_res_id
END
GO
IF (OBJECT_ID('x_audit_map_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_group_id
END
GO
IF (OBJECT_ID('x_audit_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_audit_map] DROP CONSTRAINT x_audit_map_FK_added_by_id
END
GO
IF (OBJECT_ID('x_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_asset_FK_upd_by_id] DROP CONSTRAINT x_asset_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_asset_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_asset] DROP CONSTRAINT x_asset_FK_added_by_id
END
GO
IF (OBJECT_ID('vx_trx_log') IS NOT NULL)
BEGIN
    DROP VIEW [dbo].[vx_trx_log]
END
GO
IF (OBJECT_ID('x_perm_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_perm_map]
END
GO
IF (OBJECT_ID('x_audit_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_audit_map]
END
GO
IF (OBJECT_ID('x_trx_log') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_trx_log]
END
GO
IF (OBJECT_ID('x_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_resource]
END
GO
IF (OBJECT_ID('x_policy_export_audit') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_export_audit]
END
GO
IF (OBJECT_ID('x_group_users') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group_users]
END
GO

IF (OBJECT_ID('x_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_user]
END
GO
IF (OBJECT_ID('x_group_groups') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group_groups]
END
GO
IF (OBJECT_ID('x_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_group]
END
GO
IF (OBJECT_ID('x_db_base') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_db_base]
END
GO
IF (OBJECT_ID('x_cred_store') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_cred_store]
END
GO
IF (OBJECT_ID('x_auth_sess') IS NOT NULL)
BEGIN
   DROP TABLE [dbo].[x_auth_sess]
END
GO
IF (OBJECT_ID('x_asset') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_asset]
END
GO
IF (OBJECT_ID('xa_access_audit') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[xa_access_audit]
END
GO
IF (OBJECT_ID('x_portal_user_role') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_portal_user_role]
END
GO
IF (OBJECT_ID('x_portal_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_portal_user]
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_portal_user](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[first_name] [varchar](256) DEFAULT NULL NULL,
	[last_name] [varchar](256) DEFAULT NULL NULL,
	[pub_scr_name] [varchar](2048) DEFAULT NULL NULL,
	[login_id] [varchar](767) DEFAULT NULL NULL,
	[password] [varchar](512) NOT NULL,
	[email] [varchar](512) DEFAULT NULL NULL,
	[status] [int] DEFAULT 0 NOT NULL,
	[user_src] [int] DEFAULT 0 NOT NULL,
	[notes] [varchar](4000) DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_portal_user$x_portal_user_UK_email] UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [x_portal_user$x_portal_user_UK_login_id] UNIQUE NONCLUSTERED 
(
	[login_id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_PADDING OFF
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_group](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[group_name] [varchar](1024) NOT NULL,
	[descr] [varchar](4000) NOT NULL,
	[status] [int] DEFAULT 0  NOT NULL,
	[group_type] [int] DEFAULT 0 NOT NULL,
	[cred_store_id] [bigint] DEFAULT NULL NULL,
	[group_src] [int] DEFAULT 0 NOT NULL,
	[is_visible] [int] DEFAULT 1 NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_group_groups](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[group_name] [varchar](1024) NOT NULL,
	[p_group_id] [bigint] DEFAULT NULL  NULL,
	[group_id] [bigint] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_user](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[user_name] [varchar](1024) NOT NULL,
	[descr] [varchar](4000) NOT NULL,
	[status] [int] DEFAULT 0 NOT NULL,
	[cred_store_id] [bigint] DEFAULT NULL NULL,
	[is_visible] [int] DEFAULT 1 NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_group_users](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[group_name] [varchar](1024) NOT NULL,
	[p_group_id] [bigint] DEFAULT NULL NULL,
	[user_id] [bigint] DEFAULT NULL NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE VIEW [dbo].[vx_trx_log] AS 
select x_trx_log.id AS id,x_trx_log.create_time AS create_time,x_trx_log.update_time AS update_time,x_trx_log.added_by_id AS added_by_id,x_trx_log.upd_by_id AS upd_by_id,x_trx_log.class_type AS class_type,x_trx_log.object_id AS object_id,x_trx_log.parent_object_id AS parent_object_id,x_trx_log.parent_object_class_type AS parent_object_class_type,x_trx_log.attr_name AS attr_name,x_trx_log.parent_object_name AS parent_object_name,x_trx_log.object_name AS object_name,x_trx_log.prev_val AS prev_val,x_trx_log.new_val AS new_val,x_trx_log.trx_id AS trx_id,x_trx_log.action AS action,x_trx_log.sess_id AS sess_id,x_trx_log.req_id AS req_id,x_trx_log.sess_type AS sess_type from x_trx_log 
where id in(select min(x_trx_log.id) from x_trx_log group by x_trx_log.trx_id)
GO

ALTER TABLE [dbo].[x_asset]  WITH CHECK ADD  CONSTRAINT [x_asset_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_asset] CHECK CONSTRAINT [x_asset_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_asset]  WITH CHECK ADD  CONSTRAINT [x_asset_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_asset] CHECK CONSTRAINT [x_asset_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_group_id]
GO
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_res_id] FOREIGN KEY([res_id])
REFERENCES [dbo].[x_resource] ([id])
GO
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_res_id]
GO
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_audit_map]  WITH CHECK ADD  CONSTRAINT [x_audit_map_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
GO
ALTER TABLE [dbo].[x_audit_map] CHECK CONSTRAINT [x_audit_map_FK_user_id]
GO
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_auth_sess]  WITH CHECK ADD  CONSTRAINT [x_auth_sess_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_auth_sess] CHECK CONSTRAINT [x_auth_sess_FK_user_id]
GO
ALTER TABLE [dbo].[x_cred_store]  WITH CHECK ADD  CONSTRAINT [x_cred_store_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_cred_store] CHECK CONSTRAINT [x_cred_store_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_cred_store]  WITH CHECK ADD  CONSTRAINT [x_cred_store_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_cred_store] CHECK CONSTRAINT [x_cred_store_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_db_base]  WITH CHECK ADD  CONSTRAINT [x_db_base_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_db_base] CHECK CONSTRAINT [x_db_base_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_db_base]  WITH CHECK ADD  CONSTRAINT [x_db_base_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_db_base] CHECK CONSTRAINT [x_db_base_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_cred_store_id] FOREIGN KEY([cred_store_id])
REFERENCES [dbo].[x_cred_store] ([id])
GO
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_cred_store_id]
GO
ALTER TABLE [dbo].[x_group]  WITH CHECK ADD  CONSTRAINT [x_group_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group] CHECK CONSTRAINT [x_group_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_group_id]
GO
ALTER TABLE [dbo].[x_group_groups]  WITH CHECK ADD  CONSTRAINT [x_group_groups_FK_p_group_id] FOREIGN KEY([p_group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_group_groups] CHECK CONSTRAINT [x_group_groups_FK_p_group_id]
GO
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_p_group_id] FOREIGN KEY([p_group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_p_group_id]
GO
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_group_users]  WITH CHECK ADD  CONSTRAINT [x_group_users_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
GO
ALTER TABLE [dbo].[x_group_users] CHECK CONSTRAINT [x_group_users_FK_user_id]
GO
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_group_id]
GO
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_res_id] FOREIGN KEY([res_id])
REFERENCES [dbo].[x_resource] ([id])
GO
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_res_id]
GO
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_perm_map]  WITH CHECK ADD  CONSTRAINT [x_perm_map_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
GO
ALTER TABLE [dbo].[x_perm_map] CHECK CONSTRAINT [x_perm_map_FK_user_id]
GO
ALTER TABLE [dbo].[x_policy_export_audit]  WITH CHECK ADD  CONSTRAINT [x_policy_export_audit_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_export_audit] CHECK CONSTRAINT [x_policy_export_audit_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_policy_export_audit]  WITH CHECK ADD  CONSTRAINT [x_policy_export_audit_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_export_audit] CHECK CONSTRAINT [x_policy_export_audit_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_portal_user]  WITH CHECK ADD  CONSTRAINT [x_portal_user_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_portal_user] CHECK CONSTRAINT [x_portal_user_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_portal_user]  WITH CHECK ADD  CONSTRAINT [x_portal_user_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_portal_user] CHECK CONSTRAINT [x_portal_user_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_portal_user_role]  WITH CHECK ADD  CONSTRAINT [x_portal_user_role_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_portal_user_role] CHECK CONSTRAINT [x_portal_user_role_FK_user_id]
GO
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_asset_id] FOREIGN KEY([asset_id])
REFERENCES [dbo].[x_asset] ([id])
GO
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_asset_id]
GO
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_parent_id] FOREIGN KEY([parent_id])
REFERENCES [dbo].[x_resource] ([id])
GO
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_parent_id]
GO
ALTER TABLE [dbo].[x_resource]  WITH CHECK ADD  CONSTRAINT [x_resource_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_resource] CHECK CONSTRAINT [x_resource_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_trx_log]  WITH CHECK ADD  CONSTRAINT [x_trx_log_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_trx_log] CHECK CONSTRAINT [x_trx_log_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_trx_log]  WITH CHECK ADD  CONSTRAINT [x_trx_log_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_trx_log] CHECK CONSTRAINT [x_trx_log_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_cred_store_id] FOREIGN KEY([cred_store_id])
REFERENCES [dbo].[x_cred_store] ([id])
GO
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_cred_store_id]
GO
ALTER TABLE [dbo].[x_user]  WITH CHECK ADD  CONSTRAINT [x_user_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_user] CHECK CONSTRAINT [x_user_FK_upd_by_id]
GO

CREATE NONCLUSTERED INDEX [x_asset_cr_time] ON [x_asset]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_asset_FK_added_by_id] ON [x_asset]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_asset_FK_upd_by_id] ON [x_asset]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_asset_up_time] ON [x_asset]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_audit_map_cr_time] ON [x_audit_map]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_audit_map_FK_added_by_id] ON [x_audit_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
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
GO
CREATE NONCLUSTERED INDEX [x_audit_map_FK_upd_by_id] ON [x_audit_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_audit_map_FK_user_id] ON [x_audit_map]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_audit_map_up_time] ON [x_audit_map]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_auth_sess_cr_time] ON [x_auth_sess]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_added_by_id] ON [x_auth_sess]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_upd_by_id] ON [x_auth_sess]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_auth_sess_FK_user_id] ON [x_auth_sess]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_auth_sess_up_time] ON [x_auth_sess]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_cred_store_cr_time] ON [x_cred_store]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_cred_store_FK_added_by_id] ON [x_cred_store]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_cred_store_FK_upd_by_id] ON [x_cred_store]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_cred_store_up_time] ON [x_cred_store]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_db_base_cr_time] ON [x_db_base]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_db_base_FK_added_by_id] ON [x_db_base]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_db_base_FK_upd_by_id] ON [x_db_base]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_db_base_up_time] ON [x_db_base]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_cr_time] ON [x_group]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_FK_added_by_id] ON [x_group]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_FK_cred_store_id] ON [x_group]
(
   [cred_store_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_FK_upd_by_id] ON [x_group]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_cr_time] ON [x_group_groups]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_FK_added_by_id] ON [x_group_groups]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_FK_group_id] ON [x_group_groups]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_FK_p_group_id] ON [x_group_groups]
(
   [p_group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_FK_upd_by_id] ON [x_group_groups]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_groups_up_time] ON [x_group_groups]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_up_time] ON [x_group]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_cr_time] ON [x_group_users]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_FK_added_by_id] ON [x_group_users]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_FK_p_group_id] ON [x_group_users]
(
   [p_group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_FK_upd_by_id] ON [x_group_users]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_FK_user_id] ON [x_group_users]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_group_users_up_time] ON [x_group_users]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_cr_time] ON [x_perm_map]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_FK_added_by_id] ON [x_perm_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_FK_group_id] ON [x_perm_map]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_FK_res_id] ON [x_perm_map]
(
   [res_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_FK_upd_by_id] ON [x_perm_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_FK_user_id] ON [x_perm_map]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_perm_map_up_time] ON [x_perm_map]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_export_audit_cr_time] ON [x_policy_export_audit]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_export_audit_FK_added_by_id] ON [x_policy_export_audit]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_export_audit_FK_upd_by_id] ON [x_policy_export_audit]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_export_audit_up_time] ON [x_policy_export_audit]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_cr_time] ON [x_portal_user]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_email] ON [x_portal_user]
(
   [email] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_FK_added_by_id] ON [x_portal_user]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_FK_upd_by_id] ON [x_portal_user]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_name] ON [x_portal_user]
(
   [first_name] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_role_cr_time] ON [x_portal_user_role]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_added_by_id] ON [x_portal_user_role]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_upd_by_id] ON [x_portal_user_role]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_role_FK_user_id] ON [x_portal_user_role]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_role_up_time] ON [x_portal_user_role]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_portal_user_up_time] ON [x_portal_user]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_cr_time] ON [x_resource]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_FK_added_by_id] ON [x_resource]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_FK_asset_id] ON [x_resource]
(
   [asset_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_FK_parent_id] ON [x_resource]
(
   [parent_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_FK_upd_by_id] ON [x_resource]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_up_time] ON [x_resource]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_trx_log_cr_time] ON [x_trx_log]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_trx_log_FK_added_by_id] ON [x_trx_log]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_trx_log_FK_upd_by_id] ON [x_trx_log]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_trx_log_up_time] ON [x_trx_log]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_user_cr_time] ON [x_user]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_user_FK_added_by_id] ON [x_user]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_user_FK_cred_store_id] ON [x_user]
(
   [cred_store_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 

GO
CREATE NONCLUSTERED INDEX [x_user_FK_upd_by_id] ON [x_user]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_user_up_time] ON [x_user]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [xa_access_audit_cr_time] ON [xa_access_audit]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [xa_access_audit_event_time] ON [xa_access_audit]
(
   [event_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [xa_access_audit_added_by_id] ON [xa_access_audit]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [xa_access_audit_upd_by_id] ON [xa_access_audit]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [xa_access_audit_up_time] ON [xa_access_audit]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF,DROP_EXISTING = OFF,IGNORE_DUP_KEY = OFF,ONLINE = OFF) ON [PRIMARY]
GO
IF (OBJECT_ID('x_service_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_def] DROP CONSTRAINT x_service_def_FK_added_by_id
END
GO
IF (OBJECT_ID('x_service_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_def] DROP CONSTRAINT x_service_def_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_added_by_id
END
GO
IF (OBJECT_ID('x_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_service_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service] DROP CONSTRAINT x_service_FK_type
END
GO
IF (OBJECT_ID('x_policy_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_added_by_id
END
GO
IF (OBJECT_ID('x_policy_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_policy_FK_service') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_service
END
GO
IF (OBJECT_ID('x_service_config_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_config_def] DROP CONSTRAINT x_service_config_def_FK_defid
END
GO
IF (OBJECT_ID('x_resource_def_FK_parent') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource_def] DROP CONSTRAINT x_resource_def_FK_parent
END
GO
IF (OBJECT_ID('x_resource_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_resource_def] DROP CONSTRAINT x_resource_def_FK_defid
END
GO
IF (OBJECT_ID('x_access_type_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_access_type_def] DROP CONSTRAINT x_access_type_def_FK_defid
END
GO
IF (OBJECT_ID('x_atd_grants_FK_atdid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_access_type_def_grants] DROP CONSTRAINT x_atd_grants_FK_atdid
END
GO
IF (OBJECT_ID('x_policy_condition_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_condition_def] DROP CONSTRAINT x_policy_condition_def_FK_defid
END
GO
IF (OBJECT_ID('x_enum_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_enum_def] DROP CONSTRAINT x_enum_def_FK_defid
END
GO
IF (OBJECT_ID('x_enum_element_def_FK_defid') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_enum_element_def] DROP CONSTRAINT x_enum_element_def_FK_defid
END
GO
IF (OBJECT_ID('x_service_config_map_FK_') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_config_map] DROP CONSTRAINT x_service_config_map_FK_
END
GO
IF (OBJECT_ID('x_policy_resource_FK_policy_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource] DROP CONSTRAINT x_policy_resource_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_resource_FK_res_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource] DROP CONSTRAINT x_policy_resource_FK_res_def_id
END
GO
IF (OBJECT_ID('x_policy_resource_map_FK_resource_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_resource_map] DROP CONSTRAINT x_policy_resource_map_FK_resource_id
END
GO
IF (OBJECT_ID('x_policy_item_FK_policy_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item] DROP CONSTRAINT x_policy_item_FK_policy_id
END
GO
IF (OBJECT_ID('x_policy_item_access_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_access] DROP CONSTRAINT x_policy_item_access_FK_pi_id
END
GO
IF (OBJECT_ID('x_policy_item_access_FK_atd_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_access] DROP CONSTRAINT x_policy_item_access_FK_atd_id
END
GO
IF (OBJECT_ID('x_policy_item_condition_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_condition] DROP CONSTRAINT x_policy_item_condition_FK_pi_id
END
GO
IF (OBJECT_ID('x_policy_item_condition_FK_pcd_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_condition] DROP CONSTRAINT x_policy_item_condition_FK_pcd_id
END
GO
IF (OBJECT_ID('x_policy_item_user_perm_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_user_perm] DROP CONSTRAINT x_policy_item_user_perm_FK_pi_id
END
GO
IF (OBJECT_ID('x_policy_item_user_perm_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_user_perm] DROP CONSTRAINT x_policy_item_user_perm_FK_user_id
END
GO
IF (OBJECT_ID('x_policy_item_group_perm_FK_pi_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_group_perm] DROP CONSTRAINT x_policy_item_group_perm_FK_pi_id
END
GO
IF (OBJECT_ID('x_policy_item_group_perm_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy_item_group_perm] DROP CONSTRAINT x_policy_item_group_perm_FK_group_id
END
GO
IF (OBJECT_ID('x_service_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_def]
END
GO
IF (OBJECT_ID('x_service') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service]
END
GO
IF (OBJECT_ID('x_policy') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy]
END
GO
IF (OBJECT_ID('x_service_config_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_config_def]
END
GO
IF (OBJECT_ID('x_resource_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_resource_def]
END
GO
IF (OBJECT_ID('x_access_type_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_access_type_def]
END
GO
IF (OBJECT_ID('x_access_type_def_grants') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_access_type_def_grants]
END
GO
IF (OBJECT_ID('x_policy_condition_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_condition_def]
END
GO
IF (OBJECT_ID('x_enum_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_enum_def]
END
GO
IF (OBJECT_ID('x_enum_element_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_enum_element_def]
END
GO
IF (OBJECT_ID('x_service_config_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_config_map]
END
GO
IF (OBJECT_ID('x_policy_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_resource]
END
GO
IF (OBJECT_ID('x_policy_resource_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_resource_map]
END
GO
IF (OBJECT_ID('x_policy_item') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item]
END
GO
IF (OBJECT_ID('x_policy_item_access') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_access]
END
GO
IF (OBJECT_ID('x_policy_item_condition') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_condition]
END
GO
IF (OBJECT_ID('x_policy_item_user_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_user_perm]
END
GO
IF (OBJECT_ID('x_policy_item_group_perm') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_policy_item_group_perm]
END
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_service_def](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[version] [bigint] DEFAULT NULL NULL,
	[name] [varchar](1024) DEFAULT NULL NULL,
	[impl_class_name] [varchar](1024) DEFAULT NULL NULL,
	[label] [varchar](1024) DEFAULT NULL NULL,
	[description] [varchar](1024) DEFAULT NULL NULL,
	[rb_key_label] [varchar](1024) DEFAULT NULL NULL,
	[rb_key_description] [varchar](1024) DEFAULT NULL NULL,
	[is_enabled] [tinyint] DEFAULT 1 NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[policy_version] [bigint] DEFAULT NULL NULL,
	[policy_update_time] [datetime2] DEFAULT NULL NULL,
	[description] [varchar](1024) DEFAULT NULL NULL,
	[is_enabled] [tinyint] DEFAULT 0 NOT NULL,                        
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
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[version] [bigint] DEFAULT NULL NULL,
	[service] [bigint] DEFAULT NULL NULL,
	[name] [varchar](512) DEFAULT NULL NULL, 
	[policy_type] [int] DEFAULT 0 NULL,
	[description] [varchar](1024) DEFAULT NULL NULL,
	[resource_signature] [varchar](128) DEFAULT NULL NULL,
	[is_enabled] [tinyint] DEFAULT 0 NOT NULL,
	[is_audit_enabled] [tinyint] DEFAULT 0 NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL, 
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0 NULL, 
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL,
    PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_resource_map] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[resource_id] [bigint] NOT NULL, 
	[value] [varchar](1024) DEFAULT NULL NULL,  
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_item] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[policy_id] [bigint] NOT NULL,
	[delegate_admin] [tinyint] DEFAULT 0 NOT NULL,
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0 NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_item_user_perm] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[policy_item_id] [bigint] NOT NULL,
	[user_id] [bigint]  DEFAULT NULL NULL,
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_policy_item_group_perm] (
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](1024) DEFAULT NULL NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[policy_item_id] [bigint] NOT NULL,
	[group_id] [bigint]  DEFAULT NULL NULL,
	[sort_order] [tinyint] DEFAULT 0  NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
ALTER TABLE [dbo].[x_service_def]  WITH CHECK ADD  CONSTRAINT [x_service_def_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_def] CHECK CONSTRAINT [x_service_def_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_service_def]  WITH CHECK ADD  CONSTRAINT [x_service_def_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_def] CHECK CONSTRAINT [x_service_def_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_service]  WITH CHECK ADD  CONSTRAINT [x_service_FK_type] FOREIGN KEY([type])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_service] CHECK CONSTRAINT [x_service_FK_type]
GO
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_policy]  WITH CHECK ADD  CONSTRAINT [x_policy_FK_service] FOREIGN KEY([service])
REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_policy] CHECK CONSTRAINT [x_policy_FK_service]
GO
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_config_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_config_def_FK_defid]
GO
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_conf_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_conf_def_FK_added_by]
GO
ALTER TABLE [dbo].[x_service_config_def]  WITH CHECK ADD  CONSTRAINT [x_service_conf_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_config_def] CHECK CONSTRAINT [x_service_conf_def_FK_upd_by]
GO
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_parent] FOREIGN KEY([parent])
REFERENCES [dbo].[x_resource_def] ([id])
GO
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_parent]
GO
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_defid]
GO
GO
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_added_by]
GO
ALTER TABLE [dbo].[x_resource_def]  WITH CHECK ADD  CONSTRAINT [x_resource_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_resource_def] CHECK CONSTRAINT [x_resource_def_FK_upd_by]
GO
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_defid]
GO
GO
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_added_by]
GO
ALTER TABLE [dbo].[x_access_type_def]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def] CHECK CONSTRAINT [x_access_type_def_FK_upd_by]
GO
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_atd_grants_FK_atdid] FOREIGN KEY([atd_id])
REFERENCES [dbo].[x_access_type_def] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_atd_grants_FK_atdid]
GO
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_grants_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_access_type_def_grants_FK_added_by]
GO
ALTER TABLE [dbo].[x_access_type_def_grants]  WITH CHECK ADD  CONSTRAINT [x_access_type_def_grants_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_access_type_def_grants] CHECK CONSTRAINT [x_access_type_def_grants_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_defid]
GO
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_condition_def]  WITH CHECK ADD  CONSTRAINT [x_policy_condition_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_condition_def] CHECK CONSTRAINT [x_policy_condition_def_FK_upd_by]
GO
GO
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_defid]
GO
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_added_by_id] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_added_by_id]
GO
ALTER TABLE [dbo].[x_context_enricher_def]  WITH CHECK ADD  CONSTRAINT [x_context_enricher_def_FK_upd_by_id] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_context_enricher_def] CHECK CONSTRAINT [x_context_enricher_def_FK_upd_by_id]
GO
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_defid] FOREIGN KEY([def_id])
REFERENCES [dbo].[x_service_def] ([id])
GO
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_defid]
GO
ALTER TABLE [dbo].[x_enum_element_def]  WITH CHECK ADD  CONSTRAINT [x_enum_element_def_FK_defid] FOREIGN KEY([enum_def_id])
REFERENCES [dbo].[x_enum_def] ([id])
GO
ALTER TABLE [dbo].[x_enum_element_def] CHECK CONSTRAINT [x_enum_element_def_FK_defid]
GO
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_added_by]
GO
ALTER TABLE [dbo].[x_enum_def]  WITH CHECK ADD  CONSTRAINT [x_enum_def_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_enum_def] CHECK CONSTRAINT [x_enum_def_FK_upd_by]
GO
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_] FOREIGN KEY([service])
REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_]
GO
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_added_by]
GO
ALTER TABLE [dbo].[x_service_config_map]  WITH CHECK ADD  CONSTRAINT [x_service_config_map_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_config_map] CHECK CONSTRAINT [x_service_config_map_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_policy_id] FOREIGN KEY([policy_id])
REFERENCES [dbo].[x_policy] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_policy_id]
GO
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_res_def_id] FOREIGN KEY([res_def_id])
REFERENCES [dbo].[x_resource_def] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_res_def_id]
GO
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_resource]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource] CHECK CONSTRAINT [x_policy_resource_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_resource_id] FOREIGN KEY([resource_id])
REFERENCES [dbo].[x_policy_resource] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_resource_id]
GO
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_resource_map]  WITH CHECK ADD  CONSTRAINT [x_policy_resource_map_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_resource_map] CHECK CONSTRAINT [x_policy_resource_map_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_policy_id] FOREIGN KEY([policy_id])
REFERENCES [dbo].[x_policy] ([id])
GO
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_policy_id]
GO
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_item]  WITH CHECK ADD  CONSTRAINT [x_policy_item_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item] CHECK CONSTRAINT [x_policy_item_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_pi_id]
GO
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_atd_id] FOREIGN KEY([type])
REFERENCES [dbo].[x_access_type_def] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_atd_id]
GO
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_item_access]  WITH CHECK ADD  CONSTRAINT [x_policy_item_access_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_access] CHECK CONSTRAINT [x_policy_item_access_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_pi_id]
GO
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_pcd_id] FOREIGN KEY([type])
REFERENCES [dbo].[x_policy_condition_def] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_pcd_id]
GO
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_item_condition]  WITH CHECK ADD  CONSTRAINT [x_policy_item_condition_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_condition] CHECK CONSTRAINT [x_policy_item_condition_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_pi_id]
GO
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_user_id] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_user_id]
GO
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_item_user_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_user_perm_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_user_perm] CHECK CONSTRAINT [x_policy_item_user_perm_FK_upd_by]
GO
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_pi_id] FOREIGN KEY([policy_item_id])
REFERENCES [dbo].[x_policy_item] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_pi_id]
GO
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_group_id]
GO
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_added_by] FOREIGN KEY([added_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_added_by]
GO
ALTER TABLE [dbo].[x_policy_item_group_perm]  WITH CHECK ADD  CONSTRAINT [x_policy_item_group_perm_FK_upd_by] FOREIGN KEY([upd_by_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_policy_item_group_perm] CHECK CONSTRAINT [x_policy_item_group_perm_FK_upd_by]
GO
GO
CREATE NONCLUSTERED INDEX [x_service_def_added_by_id] ON [x_service_def]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_def_upd_by_id] ON [x_service_def]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_def_cr_time] ON [x_service_def]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_def_up_time] ON [x_service_def]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_added_by_id] ON [x_service]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_upd_by_id] ON [x_service]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_cr_time] ON [x_service]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_up_time] ON [x_service]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_service_type] ON [x_service]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_added_by_id] ON [x_policy]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_upd_by_id] ON [x_policy]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
GO
CREATE NONCLUSTERED INDEX [x_policy_cr_time] ON [x_policy]
(
   [create_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_up_time] ON [x_policy]
(
   [update_time] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_service] ON [x_policy]
(
   [service] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_policy_resource_signature] ON [x_policy]
(
   [resource_signature] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
CREATE NONCLUSTERED INDEX [x_resource_def_parent] ON [x_resource_def]
(
   [parent] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY] 
GO
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Admin','','Admin','admin','ceb4f32325eda6142bd65215f4c0f371','',1);
GO
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'ROLE_SYS_ADMIN',1);
GO
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'admin',0,'Administrator');
GO
insert into x_group (ADDED_BY_ID,CREATE_TIME,DESCR,GROUP_TYPE,GROUP_NAME,STATUS,UPDATE_TIME,UPD_BY_ID) values (1,CURRENT_TIMESTAMP,'public group',0,'public',0,CURRENT_TIMESTAMP,1);
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
ALTER TABLE [dbo].[x_user_module_perm]  WITH CHECK ADD  CONSTRAINT [x_user_module_perm_FK_moduleid] FOREIGN KEY([module_id])
REFERENCES [dbo].[x_modules_master] ([id])
GO
ALTER TABLE [dbo].[x_user_module_perm]  WITH CHECK ADD  CONSTRAINT [x_user_module_perm_FK_userid] FOREIGN KEY([user_id])
REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_group_module_perm]  WITH CHECK ADD  CONSTRAINT [x_grp_module_perm_FK_module_id] FOREIGN KEY([module_id])
REFERENCES [dbo].[x_modules_master] ([id])
GO
ALTER TABLE [dbo].[x_group_module_perm]  WITH CHECK ADD  CONSTRAINT [x_grp_module_perm_FK_group_id] FOREIGN KEY([group_id])
REFERENCES [dbo].[x_group] ([id])
GO
CREATE NONCLUSTERED INDEX [x_usr_module_perm_idx_moduleid] ON [x_user_module_perm]
(
   [module_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_usr_module_perm_idx_userid] ON [x_user_module_perm]
(
   [user_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_grp_module_perm_idx_groupid] ON [x_group_module_perm]
(
   [group_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_grp_module_perm_idx_moduleid] ON [x_group_module_perm]
(
   [module_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,'Resource Based Policies','');
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,'Users/Groups','');
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,'Reports','');
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,'Audit','');
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,'Key Manager','');
GO
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangerusersync','','rangerusersync','rangerusersync','70b8374d3dfe0325aaa5002a688c7e3b','rangerusersync',1);
GO
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,2,'ROLE_SYS_ADMIN',1);
GO
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangerusersync',0,'rangerusersync');
GO
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'keyadmin','','keyadmin','keyadmin','a05f34d2dce2b4688fa82e82a89ba958','keyadmin',1);
GO
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,3,'ROLE_KEY_ADMIN',1);
GO
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'keyadmin',0,'keyadmin');
GO
insert into x_portal_user (CREATE_TIME,UPDATE_TIME,FIRST_NAME,LAST_NAME,PUB_SCR_NAME,LOGIN_ID,PASSWORD,EMAIL,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangertagsync','','rangertagsync','rangertagsync','f5820e1229418dcf2575908f2c493da5','rangertagsync',1);
GO
insert into x_portal_user_role (CREATE_TIME,UPDATE_TIME,USER_ID,USER_ROLE,STATUS) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,4,'ROLE_SYS_ADMIN',1);
GO
insert into x_user (CREATE_TIME,UPDATE_TIME,user_name,status,descr) values (CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'rangertagsync',0,'rangertagsync');
exit
