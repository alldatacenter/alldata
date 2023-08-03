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
IF (OBJECT_ID('x_tag_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_def] DROP CONSTRAINT x_tag_def_FK_added_by_id
END
GO
IF (OBJECT_ID('x_tag_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_def] DROP CONSTRAINT x_tag_def_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_tag_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_added_by_id
END
GO
IF (OBJECT_ID('x_tag_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_tag_FK_type') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag] DROP CONSTRAINT x_tag_FK_type
END
GO
IF (OBJECT_ID('x_service_res_FK_service_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_service_id
END
GO
IF (OBJECT_ID('x_service_res_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_added_by_id
END
GO
IF (OBJECT_ID('x_service_res_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource] DROP CONSTRAINT x_service_res_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_FK_res_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element] DROP CONSTRAINT x_srvc_res_el_FK_res_def_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_FK_res_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element] DROP CONSTRAINT x_srvc_res_el_FK_res_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element] DROP CONSTRAINT x_srvc_res_el_FK_added_by_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element] DROP CONSTRAINT x_srvc_res_el_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_tag_attr_def_FK_tag_def_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr_def] DROP CONSTRAINT x_tag_attr_def_FK_tag_def_id
END
GO
IF (OBJECT_ID('x_tag_attr_def_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr_def] DROP CONSTRAINT x_tag_attr_def_FK_added_by_id
END
GO
IF (OBJECT_ID('x_tag_attr_def_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr_def] DROP CONSTRAINT x_tag_attr_def_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_tag_attr_FK_tag_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr] DROP CONSTRAINT x_tag_attr_FK_tag_id
END
GO
IF (OBJECT_ID('x_tag_attr_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr] DROP CONSTRAINT x_tag_attr_FK_added_by_id
END
GO
IF (OBJECT_ID('x_tag_attr_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_attr] DROP CONSTRAINT x_tag_attr_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_tag_res_map_FK_tag_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_tag_id
END
GO
IF (OBJECT_ID('x_tag_attr_FK_tag_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_res_id
END
GO
IF (OBJECT_ID('x_tag_res_map_FK_added_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_added_by_id
END
GO
IF (OBJECT_ID('x_tag_res_map_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_tag_resource_map] DROP CONSTRAINT x_tag_res_map_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_val_FK_res_el_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element_val] DROP CONSTRAINT x_srvc_res_el_val_FK_res_el_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_val_FK_add_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element_val] DROP CONSTRAINT x_srvc_res_el_val_FK_add_by_id
END
GO
IF (OBJECT_ID('x_srvc_res_el_val_FK_upd_by_id') IS NOT NULL)
BEGIN
    ALTER TABLE [dbo].[x_service_resource_element_val] DROP CONSTRAINT x_srvc_res_el_val_FK_upd_by_id
END
GO
IF (OBJECT_ID('x_service_resource_element_val') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_resource_element_val]
END
GO
IF (OBJECT_ID('x_tag_resource_map') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_resource_map]
END
GO
IF (OBJECT_ID('x_tag_attr') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_attr]
END
GO
IF (OBJECT_ID('x_tag_attr_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_attr_def]
END
GO
IF (OBJECT_ID('x_service_resource_element') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_resource_element]
END
GO
IF (OBJECT_ID('x_service_resource') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_service_resource]
END
GO
IF (OBJECT_ID('x_tag') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag]
END
GO
IF (OBJECT_ID('x_tag_def') IS NOT NULL)
BEGIN
    DROP TABLE [dbo].[x_tag_def]
END
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_tag](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[guid] [varchar](64) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[version] [bigint] DEFAULT NULL NULL,
	[type] [bigint] NOT NULL,
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_tag$x_tag_UK_guid] UNIQUE NONCLUSTERED 
(
	[guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
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
	PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
CONSTRAINT [x_service_resource$x_service_res_UK_guid] UNIQUE NONCLUSTERED 
(
	[guid] ASC
)WITH (PAD_INDEX = OFF,STATISTICS_NORECOMPUTE = OFF,IGNORE_DUP_KEY = OFF,ALLOW_ROW_LOCKS = ON,ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_service_resource_element](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[res_id] [bigint] NOT NULL,
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
CREATE TABLE [dbo].[x_tag_attr_def](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[tag_def_id] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[type] [varchar](50) NOT NULL,
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
CREATE TABLE [dbo].[x_tag_attr](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[tag_id] [bigint] NOT NULL,
	[name] [varchar](255) NOT NULL,
	[value] [varchar](512) DEFAULT NULL NULL,
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
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[x_service_resource_element_val](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[create_time] [datetime2] DEFAULT NULL NULL,
	[update_time] [datetime2] DEFAULT NULL NULL,
	[added_by_id] [bigint] DEFAULT NULL NULL,
	[upd_by_id] [bigint] DEFAULT NULL NULL,
	[res_element_id] [bigint] NOT NULL,
	[value] [varchar](1024) NOT NULL,
	[sort_order] [int] DEFAULT 0 NULL,
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
ALTER TABLE [dbo].[x_tag_def] WITH CHECK ADD CONSTRAINT [x_tag_def_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_def] WITH CHECK ADD CONSTRAINT [x_tag_def_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag] WITH CHECK ADD CONSTRAINT [x_tag_FK_type] FOREIGN KEY([type]) REFERENCES [dbo].[x_tag_def] ([id])
GO
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_resource] WITH CHECK ADD CONSTRAINT [x_service_res_FK_service_id] FOREIGN KEY([service_id]) REFERENCES [dbo].[x_service] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_FK_res_def_id] FOREIGN KEY([res_def_id]) REFERENCES [dbo].[x_resource_def] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_FK_res_id] FOREIGN KEY([res_id]) REFERENCES [dbo].[x_service_resource] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr_def] WITH CHECK ADD CONSTRAINT [x_tag_attr_def_FK_tag_def_id] FOREIGN KEY([tag_def_id]) REFERENCES [dbo].[x_tag_def] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr_def] WITH CHECK ADD CONSTRAINT [x_tag_attr_def_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr_def] WITH CHECK ADD CONSTRAINT [x_tag_attr_def_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr] WITH CHECK ADD CONSTRAINT [x_tag_attr_FK_tag_id] FOREIGN KEY([tag_id]) REFERENCES [dbo].[x_tag] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr] WITH CHECK ADD CONSTRAINT [x_tag_attr_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_attr] WITH CHECK ADD CONSTRAINT [x_tag_attr_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_tag_id] FOREIGN KEY([tag_id]) REFERENCES [dbo].[x_tag] ([id])
GO
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_res_id] FOREIGN KEY([res_id]) REFERENCES [dbo].[x_service_resource] ([id])
GO
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_added_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_tag_resource_map] WITH CHECK ADD CONSTRAINT [x_tag_res_map_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element_val] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_val_FK_res_el_id] FOREIGN KEY([res_element_id]) REFERENCES [dbo].[x_service_resource_element] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element_val] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_val_FK_add_by_id] FOREIGN KEY([added_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
ALTER TABLE [dbo].[x_service_resource_element_val] WITH CHECK ADD CONSTRAINT [x_srvc_res_el_val_FK_upd_by_id] FOREIGN KEY([upd_by_id]) REFERENCES [dbo].[x_portal_user] ([id])
GO
INSERT INTO x_modules_master(create_time,update_time,added_by_id,upd_by_id,module,url) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,dbo.getXportalUIdByLoginId('admin'),dbo.getXportalUIdByLoginId('admin'),'Tag Based Policies','');
GO
CREATE NONCLUSTERED INDEX [x_tag_def_IDX_added_by_id] ON [x_tag_def]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_def_IDX_upd_by_id] ON [x_tag_def]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_IDX_type] ON [x_tag]
(
   [type] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_IDX_added_by_id] ON [x_tag]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_IDX_upd_by_id] ON [x_tag]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_service_res_IDX_added_by_id] ON [x_service_resource]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_service_res_IDX_upd_by_id] ON [x_service_resource]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_srvc_res_el_IDX_added_by_id] ON [x_service_resource_element]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_srvc_res_el_IDX_upd_by_id] ON [x_service_resource_element]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_def_IDX_tag_def_id] ON [x_tag_attr_def]
(
   [tag_def_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_def_IDX_added_by_id] ON [x_tag_attr_def]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_def_IDX_upd_by_id] ON [x_tag_attr_def]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_IDX_tag_id] ON [x_tag_attr]
(
   [tag_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_IDX_added_by_id] ON [x_tag_attr]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_attr_IDX_upd_by_id] ON [x_tag_attr]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_tag_id] ON [x_tag_resource_map]
(
   [tag_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_res_id] ON [x_tag_resource_map]
(
   [res_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_added_by_id] ON [x_tag_resource_map]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_tag_res_map_IDX_upd_by_id] ON [x_tag_resource_map]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_srvc_res_el_val_IDX_resel_id] ON [x_service_resource_element_val]
(
   [res_element_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_srvc_res_el_val_IDX_addby_id] ON [x_service_resource_element_val]
(
   [added_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [x_srvc_res_el_val_IDX_updby_id] ON [x_service_resource_element_val]
(
   [upd_by_id] ASC
)
WITH (SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF) ON [PRIMARY]
GO

IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_service_def' and column_name = 'def_options')
BEGIN
	ALTER TABLE [dbo].[x_service_def] ADD [def_options] [varchar](1024) DEFAULT NULL NULL;
END
GO

IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_policy_item' and column_name in('item_type','is_enabled','comments'))
BEGIN
	ALTER TABLE [dbo].[x_policy_item] ADD [item_type] [int] DEFAULT 0 NOT NULL,[is_enabled] [tinyint] DEFAULT 1 NOT NULL,[comments] [varchar](255) DEFAULT NULL NULL;
END
GO

IF NOT EXISTS(select * from INFORMATION_SCHEMA.columns where table_name = 'x_service' and column_name in('tag_service','tag_version','tag_update_time'))
BEGIN
	ALTER TABLE [dbo].[x_service] ADD [tag_service] [bigint] DEFAULT NULL NULL,[tag_version] [bigint] DEFAULT 0 NOT NULL,[tag_update_time] [datetime2] DEFAULT NULL NULL,CONSTRAINT [x_service_FK_tag_service] FOREIGN KEY([tag_service]) REFERENCES [dbo].[x_service] ([id]);
END
GO
exit
