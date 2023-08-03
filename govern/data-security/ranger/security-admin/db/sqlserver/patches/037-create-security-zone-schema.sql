-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

GO
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy_export_audit' and column_name = 'zone_name')
BEGIN
	ALTER TABLE [dbo].[x_policy_export_audit] ADD [zone_name] [varchar](255) DEFAULT NULL NULL;
END
GO
IF (OBJECT_ID('x_policy_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] DROP CONSTRAINT x_policy_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_group_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_added_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_group_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_group_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_group_FK_group_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_group] DROP CONSTRAINT x_sz_ref_group_FK_group_id
END
GO
IF (OBJECT_ID('x_sz_ref_user_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_added_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_user_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_user_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_user_FK_user_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_user_id
END
GO
IF (OBJECT_ID('x_sz_ref_user_FK_user_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_user] DROP CONSTRAINT x_sz_ref_user_FK_user_name
END
GO
IF (OBJECT_ID('x_sz_ref_resource_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_added_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_resource_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_resource_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_resource_FK_resource_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_resource] DROP CONSTRAINT x_sz_ref_resource_FK_resource_def_id
END
GO
IF (OBJECT_ID('x_sz_ref_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_added_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_service_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_service_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_service_id
END
IF (OBJECT_ID('x_sz_ref_service_FK_service_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_service] DROP CONSTRAINT x_sz_ref_service_FK_service_name
END
GO
IF (OBJECT_ID('x_sz_ref_tag_service_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_added_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_tag_service_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_sz_ref_tag_service_FK_zone_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_zone_id
END
GO
IF (OBJECT_ID('x_sz_ref_tag_service_FK_tag_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_tag_service_id
END
IF (OBJECT_ID('x_sz_ref_tag_service_FK_tag_service_name') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] DROP CONSTRAINT x_sz_ref_tag_service_FK_tag_service_name
END
GO
IF (OBJECT_ID('x_ranger_global_state_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_ranger_global_state] DROP CONSTRAINT x_ranger_global_state_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_ranger_global_state_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_ranger_global_state] DROP CONSTRAINT x_ranger_global_state_FK_added_by_id
END
GO
IF (OBJECT_ID('x_security_zone_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone] DROP CONSTRAINT x_security_zone_FK_added_by_id
END
GO
IF (OBJECT_ID('x_security_zone_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_security_zone] DROP CONSTRAINT x_security_zone_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_security_zone_ref_service') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_service]
END
GO
IF (OBJECT_ID('x_security_zone_ref_tag_srvc') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_tag_srvc]
END
GO
IF (OBJECT_ID('x_security_zone_ref_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_resource]
END
GO
IF (OBJECT_ID('x_security_zone_ref_user') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_user]
END
GO
IF (OBJECT_ID('x_security_zone_ref_group') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone_ref_group]
END
GO
IF (OBJECT_ID('x_ranger_global_state') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_ranger_global_state]
END
GO
IF (OBJECT_ID('x_security_zone') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_security_zone]
END
GO
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
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_tag_service_id] FOREIGN KEY([tag_srvc_id]) REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_security_zone_ref_tag_srvc] WITH CHECK ADD CONSTRAINT [x_sz_ref_tag_service_FK_tag_service_name] FOREIGN KEY([tag_srvc_name]) REFERENCES [dbo].[x_service] ([name])
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
        [user_name] [varchar](767) DEFAULT NULL NULL,
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
        [group_name] [varchar](767) DEFAULT NULL NULL,
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
ALTER TABLE [dbo].[x_security_zone] WITH CHECK ADD CONSTRAINT [x_security_zone_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_security_zone] WITH CHECK ADD CONSTRAINT [x_security_zone_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_ranger_global_state] WITH CHECK ADD CONSTRAINT [x_ranger_global_state_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_ranger_global_state] WITH CHECK ADD CONSTRAINT [x_ranger_global_state_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
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
IF NOT EXISTS(select * from x_security_zone where id = 1 and name=' ')
BEGIN
	INSERT INTO x_security_zone(create_time, update_time, added_by_id, upd_by_id, version, name, jsonData, description) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, dbo.getXportalUIdByLoginId('admin'), dbo.getXportalUIdByLoginId('admin'), 1, ' ', '', 'Unzoned zone');
END
GO
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy' and column_name in('zone_id'))
BEGIN
	ALTER TABLE [dbo].[x_policy] ADD [zone_id] [bigint] DEFAULT 1 NOT NULL;
END
GO
IF (OBJECT_ID('x_policy_FK_zone_id') IS NULL)
BEGIN
    ALTER TABLE [dbo].[x_policy] ADD CONSTRAINT [x_policy_FK_zone_id] FOREIGN KEY([zone_id]) REFERENCES [dbo].[x_security_zone] ([id]);
END
GO
PRINT 'Created function dbo.getXportalUIdByLoginId successfully'
GO
IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy_export_audit' and column_name = 'zone_name')
BEGIN
	ALTER TABLE [dbo].[x_policy_export_audit] ADD [zone_name] [varchar](255) DEFAULT NULL NULL;
END

IF NOT EXISTS(select * from x_modules_master where module = 'Security Zone') 
BEGIN	
	INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Security Zone','');
END
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('admin') and module_id=dbo.getModulesIdByName('Security Zone')) 
BEGIN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('admin'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('rangerusersync') and module_id=dbo.getModulesIdByName('Security Zone')) 
BEGIN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangerusersync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END
GO
IF NOT EXISTS(select * from x_user_module_perm where user_id=dbo.getXportalUIdByLoginId('rangertagsync') and module_id=dbo.getModulesIdByName('Security Zone'))
BEGIN
	INSERT INTO x_user_module_perm (user_id,module_id,create_time,update_time,added_by_id,upd_by_id,is_allowed) VALUES (dbo.getXportalUIdByLoginId('rangertagsync'),dbo.getModulesIdByName('Security Zone'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),1);
END
GO
exit

